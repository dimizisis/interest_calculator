package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.Globals;
import db.DatabaseConnection;
import db.InsertToDB;
import infrastructure.interest.JavaFile;
import infrastructure.newcode.DiffEntry;
import infrastructure.newcode.PrincipalResponseEntity;
import metricsCalculator.calculator.MetricsCalculator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;

public class Main {

    public static void main(String[] args) throws Exception {

        Globals.setProjectURL(args[0]);
        Globals.setProjectPath(args[1]);
        try {
            deleteSourceCode(new File(Globals.getProjectPath()));
        } catch (Exception ignored) {}
        System.out.printf("Cloning %s...\n", args[0]);
        cloneRepository();
        if (Objects.isNull(Globals.getGit()))
            return;
        if (args.length == 2)
            InsertToDB.insertProjectToDatabase();

        System.out.println("Receiving all commit ids...");
        List<String> commitIds = getCommitIds(Globals.getProjectURL());
        if (Objects.isNull(commitIds))
            return;
        Globals.setCurrentSha(Objects.requireNonNull(commitIds.get(0)));
        checkout(commitIds.get(0), 1);
        System.out.printf("Calculating metrics for commit %s (%d)...\n", Globals.getCurrentSha(), Globals.getRevisionCount());
        setMetrics(Globals.getProjectPath());
        System.out.println("Calculated metrics for all files from first commit!");
        if (args.length == 2)
            Globals.getJavaFiles().forEach(InsertToDB::insertMetricsToDatabase);
        else
            Globals.getJavaFiles().forEach(Globals::append);

        for (int i = 1; i < commitIds.size(); ++i) {
            Globals.setCurrentSha(commitIds.get(i));
            checkout(Globals.getCurrentSha(), i+1);
            Globals.setRevision(i+1);
            System.out.printf("Calculating metrics for commit %s (%d)...\n", Globals.getCurrentSha(), Globals.getRevisionCount());
            try {
                PrincipalResponseEntity[] responseEntities = getResponseEntitiesAtCommit(Globals.getProjectURL(), Globals.getCurrentSha());
                if (Objects.isNull(responseEntities))
                    continue;
                List<DiffEntry> diffEntries = responseEntities[0].getDiffEntries();
                removeDeletedFiles(diffEntries);
                Set<JavaFile> newFiles = findNewFiles(Objects.requireNonNull(diffEntries));
                Set<JavaFile> modifiedFiles = findModifiedFiles(Objects.requireNonNull(diffEntries));
                System.out.println("Analyzing new/modified commit files...");
                setMetrics(Globals.getProjectPath(), newFiles);
                setMetrics(Globals.getProjectPath(), modifiedFiles);
                System.out.println("Calculated metrics for all files!");
                if (args.length == 2)
                    Globals.getJavaFiles().forEach(InsertToDB::insertMetricsToDatabase);
                else {
                    Globals.getJavaFiles().forEach(Globals::append);
                    Globals.compound();
                }
            } catch (Exception ignored) {}
        }
        if (args.length == 2)
            DatabaseConnection.closeConnection();
        else
            writeCSV(args[2]);
        System.out.printf("Finished analysing %d revisions.\n", Globals.getRevisionCount());
    }

    private static void writeCSV(String path) throws IOException {

        FileWriter csvWriter = new FileWriter(path);

        for (String header : Globals.getOutputHeaders())
            csvWriter.append(header);
        csvWriter.append(Globals.getOutput());

        csvWriter.flush();
        csvWriter.close();
    }

    public static void deleteSourceCode(File file) throws NullPointerException {
        if (file.isDirectory()) {
            /* If directory is empty, then delete it */
            if (Objects.requireNonNull(file.list()).length == 0)
                file.delete();
            else {
                /* List all the directory contents */
                String[] files = file.list();

                for (String temp : files) {
                    /* Construct the file structure */
                    File fileDelete = new File(file, temp);
                    /* Recursive delete */
                    deleteSourceCode(fileDelete);
                }

                /* Check the directory again, if empty then delete it */
                if (Objects.requireNonNull(file.list()).length == 0)
                    file.delete();
            }
        }
        else {
            /* If file, then delete it */
            file.delete();
        }
    }

    /**
     * Gets all commit ids for a specific git repo.
     *
     * @param gitURL the url of git repository
     */
    private static List<String> getCommitIds(String gitURL) {
        HttpResponse<JsonNode> httpResponse = null;
        Unirest.setTimeouts(0, 0);
        try {
            httpResponse = Unirest.get("http://195.251.210.147:8989/api/internal/commits-history/shas?url=" + gitURL).asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return Objects.nonNull(httpResponse) ? Arrays.asList(new Gson().fromJson(httpResponse.getBody().toString(), String[].class)) : null;
    }

    /**
     * Gets all commit ids for a specific git repo.
     *
     * @param sha the commit id
     */
    private static PrincipalResponseEntity[] getResponseEntitiesAtCommit(String gitURL, String sha) {
        HttpResponse<JsonNode> httpResponse;
        Unirest.setTimeouts(0, 0);
        try {
            httpResponse = Unirest.get("http://195.251.210.147:8989/api/internal/project-commit-changes?url=" + gitURL + "&sha=" + sha).asJson();
            return new Gson().fromJson(httpResponse.getBody().toString(), PrincipalResponseEntity[].class);
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Removes those files that are marked as 'DELETED' (new code's call)
     *
     * @param diffEntries the modified java files (new, modified, deleted)
     */
    private static Set<JavaFile> removeDeletedFiles(List<DiffEntry> diffEntries) {
        Set<JavaFile> deletedFiles = ConcurrentHashMap.newKeySet();
        diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.getChangeType().equals("DELETE"))
                .forEach(diffEntry -> {
                    deletedFiles.add(new JavaFile(diffEntry.getOldFilePath()));
                    Globals.getJavaFiles().removeIf(javaFile -> javaFile.getPath().endsWith(diffEntry.getOldFilePath()));
                });
        return deletedFiles;
    }

    /**
     * Find those files that are marked as 'NEW' (new code's call)
     *
     * @param diffEntries the modified java files (new, modified, deleted)
     * @return a set containing all the new files
     */
    private static Set<JavaFile> findNewFiles(List<DiffEntry> diffEntries) {
        Set<JavaFile> newFiles = ConcurrentHashMap.newKeySet();
        diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.getChangeType().equals("ADD"))
                .filter(diffEntry -> diffEntry.getNewFilePath().toLowerCase().endsWith(".java"))
                .forEach(diffEntry -> newFiles.add(new JavaFile(diffEntry.getNewFilePath())));
        return newFiles;
    }

    /**
     * Find those files that are marked as 'MODIFIED' (new code's call)
     *
     * @param diffEntries the modified java files (new, modified, deleted)
     * @return a set containing all the modified files
     */
    private static Set<JavaFile> findModifiedFiles(List<DiffEntry> diffEntries) {
        Set<JavaFile> modifiedFiles = ConcurrentHashMap.newKeySet();
        diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.getChangeType().equals("MODIFY"))
                .forEach(diffEntry -> {
                    for (JavaFile javaFile : Globals.getJavaFiles()) {
                        if (javaFile.getPath().endsWith(diffEntry.getOldFilePath())) {
                            javaFile.setPath(javaFile.getPath().replace(diffEntry.getOldFilePath(), diffEntry.getNewFilePath()));
                            modifiedFiles.add(javaFile);
                        }
                    }
                });
        return modifiedFiles;
    }

    /**
     * Clones a repo to a specified path.
     */
    private static void cloneRepository() {
        try {
            Globals.setGit(Git.cloneRepository()
                    .setURI(Globals.getProjectURL())
                    .setDirectory(new File(Globals.getProjectPath()))
                    .call());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checkouts to specified commitId (SHA)
     *
     * @param commitId the SHA we are checking out to
     */
    private static void checkout(String commitId, int versionNum) throws GitAPIException {
        try {
            Globals.getGit().checkout().setCreateBranch(true).setName("version" + versionNum).setStartPoint(commitId).call();
        } catch (CheckoutConflictException e) {
            deleteSourceCode(new File(Globals.getProjectPath()));
            cloneRepository();
            Globals.getGit().checkout().setCreateBranch(true).setName("version" + versionNum).setStartPoint(commitId).call();
        }
    }

    /**
     * Get Metrics from Metrics Calculator for every java file (initial calculation)
     *
     * @param projectPath the project root
     */
    private static void setMetrics(String projectPath) {
        int resultCode = MetricsCalculator.start(projectPath);
        if (resultCode == -1)
            return;
        String st = MetricsCalculator.printResults();
        MetricsCalculator.reset();
        String[] s = st.split("\\r?\\n");
        for (int i = 1; i < s.length; ++i) {
            String[] column = s[i].split(";");
            String filePath = column[0] + ".java";
            JavaFile jf = new JavaFile(filePath);
            registerMetrics(column, jf);
            Globals.addJavaFile(jf);
        }
    }

    /**
     * Get Metrics from Metrics Calculator for specific java files (new or modified)
     *
     * @param projectPath the project root
     * @param jfs         the list of java files
     */
    private static void setMetrics(String projectPath, Set<JavaFile> jfs) {
        try {
            for (JavaFile jf : jfs) {
                int resultCode = MetricsCalculator.start(projectPath, jf.getPath().replace("\\", "/"));
                if (resultCode == -1)
                    continue;
                try {
                    String st = MetricsCalculator.printResults();
                    MetricsCalculator.reset();
                    String[] s = st.split("\\r?\\n");
                    String[] column = s[1].split(";");
                    registerMetrics(column, jf);
                    Globals.addJavaFile(jf);
                } catch (ArrayIndexOutOfBoundsException ignored) {}
            }
            jfs.forEach(JavaFile::calculateInterest);
        } catch (Exception ignored) {}
    }

    /**
     * Register Metrics to specified java file
     *
     * @param calcEntries entries taken from MetricsCalculator's results
     * @param jf          the java file we are registering metrics to
     */
    private static void registerMetrics(String[] calcEntries, JavaFile jf) {
        jf.getQualityMetrics().setWMC(Double.parseDouble(calcEntries[1]));
        jf.getQualityMetrics().setDIT(Integer.parseInt(calcEntries[2]));
        jf.getQualityMetrics().setNOCC(Integer.parseInt(calcEntries[3]));
        jf.getQualityMetrics().setRFC(Double.parseDouble(calcEntries[4]));
        jf.getQualityMetrics().setLCOM(Double.parseDouble(calcEntries[5]));
        jf.getQualityMetrics().setComplexity(Double.parseDouble(calcEntries[6]));
        jf.getQualityMetrics().setNOM(Double.parseDouble(calcEntries[7]));
        jf.getQualityMetrics().setMPC(Integer.parseInt(calcEntries[8]));
        jf.getQualityMetrics().setDAC(Integer.parseInt(calcEntries[9]));
        jf.getQualityMetrics().setOldSIZE1(jf.getQualityMetrics().getSIZE1());
        jf.getQualityMetrics().setSIZE1(Integer.parseInt(calcEntries[10]));
        jf.getQualityMetrics().setSIZE2(Integer.parseInt(calcEntries[11]));
        jf.getQualityMetrics().setCBO(Float.parseFloat(calcEntries[12]));
        jf.getQualityMetrics().setClassesNum(Integer.parseInt(calcEntries[13]));
    }
}
