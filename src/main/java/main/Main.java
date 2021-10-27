package main;

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

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import metricsCalculator.calculator.MetricsCalculator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;

public class Main {
    public static void main(String[] args) throws Exception {
        FileInputStream file = new FileInputStream(args[1]);
        XSSFWorkbook xSSFWorkbook = new XSSFWorkbook(file);
        Sheet sheet = xSSFWorkbook.getSheetAt(0);
        for (Row row : sheet) {
            Globals.setProjectURL("https://github.com/apache/" + row.getCell(0).getStringCellValue());
            Globals.setProjectOwner();
            Globals.setProjectRepo();
            Globals.setProjectPath(args[0] + "/" + "apache" + "_" + Globals.getProjectRepo());
            Globals.setRevisionCount(1);
            System.out.println("Receiving all commit ids...");
            List<String> commitIds = getCommitIds(row.getCell(1).getStringCellValue());
            System.out.println("Total commits: " + commitIds.size());
            if (commitIds.isEmpty())
                continue;
            try {
                deleteSourceCode(new File(Globals.getProjectPath()));
            } catch (Exception exception) {
            }
            System.out.printf("Cloning %s...\n", Globals.getProjectURL());
            cloneRepository();
            if (Objects.isNull(Globals.getGit()))
                return;
            Globals.setCurrentSha(Objects.requireNonNull(commitIds.get(0)));
            checkout(commitIds.get(0), Globals.getRevisionCount());
            System.out.printf("Calculating metrics for commit %s (%d)...\n", Globals.getCurrentSha(), Globals.getRevisionCount());
            setMetrics(Globals.getProjectPath());
            System.out.println("Calculated metrics for all files from first commit!");
            insertFirstData();
            DatabaseConnection.getConnection().commit();
            Globals.setRevisionCount(Globals.getRevisionCount() + 1);
            for (int i = 1; i < commitIds.size(); ++i) {
                Globals.setCurrentSha(commitIds.get(i));
                checkout(Globals.getCurrentSha(), Globals.getRevisionCount());
                System.out.printf("Calculating metrics for commit %s (%d)...\n", Globals.getCurrentSha(), Globals.getRevisionCount());
                try {
                    DiffEntry[] diffEntries = getResponseEntitiesBetweenCommits(Globals.getProjectURL(), commitIds.get(i - 1), commitIds.get(i));
                    if (Objects.isNull(diffEntries) || diffEntries.length == 0) {
                        if (Globals.getJavaFiles().isEmpty())
                            InsertToDB.insertEmpty();
                        else
                            insertData();
                        System.out.println("Calculated metrics for all files!");
                    } else {
                        System.out.println("Analyzing new/modified commit files...");
                        setMetrics(Arrays.asList(diffEntries));
                        System.out.println("Calculated metrics for all files!");
                        insertData();
                    }
                    DatabaseConnection.getConnection().commit();
                } catch (Exception exception) {
                    DatabaseConnection.getConnection().commit();
                }
                Globals.setRevisionCount(Globals.getRevisionCount() + 1);
            }
            System.out.printf("Finished analysing %d revisions.\n", Globals.getRevisionCount() - 1);
            Globals.getJavaFiles().clear();
        }
        DatabaseConnection.closeConnection(true);
    }

    private static void insertFirstData() {
        InsertToDB.insertProjectToDatabase();
        if (Globals.getJavaFiles().size() == 0) {
            InsertToDB.insertEmpty();
        } else {
            Globals.getJavaFiles().forEach(InsertToDB::insertFileToDatabase);
            Globals.getJavaFiles().forEach(InsertToDB::insertMetricsToDatabase);
        }
    }

    private static void insertData() {
        if (Globals.getJavaFiles().size() == 0) {
            InsertToDB.insertEmpty();
        } else {
            Globals.getJavaFiles().forEach(InsertToDB::insertFileToDatabase);
            Globals.getJavaFiles().forEach(InsertToDB::insertMetricsToDatabase);
        }

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
        } else {
            /* If file, then delete it */
            file.delete();
        }
    }

    private static List<String> getCommitIds(String commits) {
        return Arrays.asList(commits.split(","));
    }

    private static DiffEntry[] getResponseEntitiesBetweenCommits(String gitURL, String startSha, String endSha) {
        Unirest.setTimeouts(0L, 0L);
        try {
            HttpResponse<JsonNode> httpResponse = Unirest.get("http://195.251.210.147:4114/api/nikolaidis/diff-between-commits?url=" + gitURL + "&parent=" + startSha + "&commit=" + endSha).asJson();
            return (new Gson()).fromJson(httpResponse.getBody().toString(), DiffEntry[].class);
        } catch (UnirestException e) {
            e.printStackTrace();
            return null;
        }
    }

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

    private static Set<JavaFile> findNewFiles(List<DiffEntry> diffEntries) {
        Set<JavaFile> newFiles = ConcurrentHashMap.newKeySet();
        diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.getChangeType().equals("ADD") || diffEntry.getChangeType().equals("COPY"))
                .filter(diffEntry -> diffEntry.getNewFilePath().toLowerCase().endsWith(".java"))
                .forEach(diffEntry -> newFiles.add(new JavaFile(diffEntry.getNewFilePath())));
        return newFiles;
    }

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

    private static Set<JavaFile> findRenamedFiles(List<DiffEntry> diffEntries) {
        Set<JavaFile> renamedFiles = ConcurrentHashMap.newKeySet();
        diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.getChangeType().equals("RENAME"))
                .forEach(diffEntry -> {
                    for (JavaFile javaFile : Globals.getJavaFiles()) {
                        if (javaFile.getPath().endsWith(diffEntry.getOldFilePath())) {
                            javaFile.setPath(javaFile.getPath().replace(diffEntry.getOldFilePath(), diffEntry.getNewFilePath()));
                            renamedFiles.add(javaFile);
                        }
                    }
                });
        return renamedFiles;
    }

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

    private static void checkout(String commitId, int versionNum) throws GitAPIException {
        try {
            Globals.getGit().checkout().setCreateBranch(true).setName("version" + versionNum).setStartPoint(commitId).call();
        } catch (CheckoutConflictException e) {
            deleteSourceCode(new File(Globals.getProjectPath()));
            cloneRepository();
            Globals.getGit().checkout().setCreateBranch(true).setName("version" + versionNum).setStartPoint(commitId).call();
        }
    }

    private static void setMetrics(List<DiffEntry> diffEntries) {
        findRenamedFiles(diffEntries);
        removeDeletedFiles(diffEntries);
        Set<JavaFile> newFiles = findNewFiles(Objects.requireNonNull(diffEntries));
        Set<JavaFile> modifiedFiles = findModifiedFiles(Objects.requireNonNull(diffEntries));
        setMetrics(Globals.getProjectPath(), newFiles);
        setMetrics(Globals.getProjectPath(), modifiedFiles);
    }

    private static void setMetrics(String projectPath) {
        int resultCode = MetricsCalculator.start(projectPath);
        if (resultCode == -1)
            return;
        String st = MetricsCalculator.printResults();
        MetricsCalculator.reset();
        String[] s = st.split("\\r?\\n");
        for (int i = 1; i < s.length; i++) {
            String[] column = s[i].split(";");
            String filePath = column[0];
            String className = column[1];
            if (Globals.getJavaFiles().stream().noneMatch(javaFile -> javaFile.getPath().equals(filePath.replace("\\", "/")))) {
                JavaFile jf = new JavaFile(filePath);
                jf.addClassName(className);
                registerMetrics(column, jf);
                Globals.addJavaFile(jf);
            } else {
                JavaFile jf = getAlreadyDefinedFile(filePath);
                if (Objects.nonNull(jf))
                    if (jf.containsClass(className)) {
                        registerMetrics(column, jf);
                    } else {
                        jf.addClassName(className);
                        appendMetrics(column, jf);
                    }
            }
        }
    }

    private static JavaFile getAlreadyDefinedFile(String filePath) {
        for (JavaFile jf : Globals.getJavaFiles()) {
            if (jf.getPath().equals(filePath))
                return jf;
        }
        return null;
    }

    private static void setMetrics(String projectPath, Set<JavaFile> jfs) {
        if (jfs.isEmpty())
            return;
        int resultCode = MetricsCalculator.start(projectPath, jfs);
        if (resultCode == -1)
            return;
        String st = MetricsCalculator.printResults();
        MetricsCalculator.reset();
        String[] s = st.split("\\r?\\n");
        try {
            for (int i = 1; i < s.length; i++) {
                String[] column = s[i].split(";");
                String filePath = column[0];
                String className = column[1];
                if (Globals.getJavaFiles().stream().noneMatch(javaFile -> javaFile.getPath().equals(filePath.replace("\\", "/")))) {
                    JavaFile jf = new JavaFile(filePath);
                    jf.addClassName(className);
                    registerMetrics(column, jf);
                    jf.calculateInterest();
                    Globals.addJavaFile(jf);
                } else {
                    JavaFile jf = getAlreadyDefinedFile(filePath);
                    if (Objects.nonNull(jf)) {
                        if (jf.containsClass(className)) {
                            registerMetrics(column, jf);
                        } else {
                            jf.addClassName(className);
                            appendMetrics(column, jf);
                        }
                        jf.calculateInterest();
                    }
                }
            }
        } catch (Exception exception) {
        }
    }

    private static void registerMetrics(String[] calcEntries, JavaFile jf) {
        jf.getQualityMetrics().setWMC(Double.parseDouble(calcEntries[2]));
        jf.getQualityMetrics().setDIT(Integer.parseInt(calcEntries[3]));
        jf.getQualityMetrics().setNOCC(Integer.parseInt(calcEntries[4]));
        jf.getQualityMetrics().setRFC(Double.parseDouble(calcEntries[5]));
        jf.getQualityMetrics().setLCOM(Double.parseDouble(calcEntries[6]));
        jf.getQualityMetrics().setComplexity(Double.parseDouble(calcEntries[7]));
        jf.getQualityMetrics().setNOM(Double.parseDouble(calcEntries[8]));
        jf.getQualityMetrics().setMPC(Double.parseDouble(calcEntries[9]));
        jf.getQualityMetrics().setDAC(Integer.parseInt(calcEntries[10]));
        jf.getQualityMetrics().setOldSIZE1(jf.getQualityMetrics().getSIZE1());
        jf.getQualityMetrics().setSIZE1(Integer.parseInt(calcEntries[11]));
        jf.getQualityMetrics().setSIZE2(Integer.parseInt(calcEntries[12]));
        jf.getQualityMetrics().setCBO(Double.parseDouble(calcEntries[13]));
        jf.getQualityMetrics().setClassesNum(jf.getClasses().size());
    }

    private static void appendMetrics(String[] calcEntries, JavaFile jf) {
        jf.getQualityMetrics().setWMC(jf.getQualityMetrics().getWMC() + Double.parseDouble(calcEntries[2]));
        jf.getQualityMetrics().setDIT(jf.getQualityMetrics().getDIT() + Integer.parseInt(calcEntries[3]));
        jf.getQualityMetrics().setNOCC(jf.getQualityMetrics().getNOCC() + Integer.parseInt(calcEntries[4]));
        jf.getQualityMetrics().setRFC(jf.getQualityMetrics().getRFC() + Double.parseDouble(calcEntries[5]));
        if (Double.parseDouble(calcEntries[6]) > 0.0D)
            jf.getQualityMetrics().setLCOM(jf.getQualityMetrics().getLCOM() + Double.parseDouble(calcEntries[6]));
        if (Double.parseDouble(calcEntries[7]) > 0.0D)
            jf.getQualityMetrics().setComplexity(jf.getQualityMetrics().getComplexity() + Double.parseDouble(calcEntries[7]));
        jf.getQualityMetrics().setNOM(jf.getQualityMetrics().getNOM() + Double.parseDouble(calcEntries[8]));
        jf.getQualityMetrics().setMPC(jf.getQualityMetrics().getMPC() + Double.parseDouble(calcEntries[9]));
        jf.getQualityMetrics().setDAC(jf.getQualityMetrics().getDAC() + Integer.parseInt(calcEntries[10]));
        jf.getQualityMetrics().setOldSIZE1(jf.getQualityMetrics().getSIZE1());
        jf.getQualityMetrics().setSIZE1(jf.getQualityMetrics().getSIZE1() + Integer.parseInt(calcEntries[11]));
        jf.getQualityMetrics().setSIZE2(jf.getQualityMetrics().getSIZE2() + Integer.parseInt(calcEntries[12]));
        jf.getQualityMetrics().setCBO(jf.getQualityMetrics().getCBO() + Double.parseDouble(calcEntries[13]));
        jf.getQualityMetrics().setClassesNum(jf.getClasses().size());
    }
}
