package main;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.Globals;
import infrastructure.interest.JavaFile;
import infrastructure.newcode.DiffEntry;
import infrastructure.newcode.PrincipalResponseEntity;
import metricsCalculator.calculator.MetricsCalculator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class Main {
    private static final String GIT_SERVICE_URL = "https://github.com/";
    private static final String OWNER = "AngelikiTsintzira";
    private static final String REPOSITORY = "Technical-Debt-Management-Toolbox";
    private static final String CLONE_PATH = "C:/Users/Dimitris/Desktop/" + REPOSITORY;

    public static void main(String[] args) throws Exception {

        Git git = cloneRepository(GIT_SERVICE_URL + OWNER + "/" + REPOSITORY, CLONE_PATH);
        PrincipalResponseEntity[] responseEntities = getResponseEntities();
        checkout(git, Objects.requireNonNull(responseEntities)[0].getSha());
        setMetrics(CLONE_PATH);

        for (int i = 1; i < Objects.requireNonNull(responseEntities).length; ++i) {
            Globals.incrementRevisions();
            checkout(git, responseEntities[i].getSha());
            removeDeletedFiles(responseEntities[i].getDiffEntries());
            Set<JavaFile> newFiles = findNewFiles(responseEntities[i].getDiffEntries());
            Set<JavaFile> modifiedFiles = findModifiedFiles(responseEntities[i].getDiffEntries());
            setMetrics(CLONE_PATH, newFiles);
            setMetrics(CLONE_PATH, modifiedFiles);
        }
        System.out.println("File count: " + Globals.getJavaFiles().size());
        for (JavaFile file : Globals.getJavaFiles()) {
            System.out.println("File: " + file.getPath());
        }

    }

    /**
     * Removes those files that are marked as 'DELETED' (new code's call)
     *
     * @param diffEntries the modified java files (new, modified, deleted)
     */
    private static void removeDeletedFiles(List<DiffEntry> diffEntries) {
        diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.getChangeType().equals("DELETE"))
                .forEach(diffEntry -> Globals.getJavaFiles().removeIf(javaFile -> javaFile.getPath().endsWith(diffEntry.getOldFilePath())));
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
     *
     * @param gitUrl    the repo's URL
     * @param clonePath the path we are cloning to
     * @return a git object (will be used for checkouts)
     */
    private static Git cloneRepository(String gitUrl, String clonePath) throws Exception {
        try {
            return Git.cloneRepository()
                    .setURI(gitUrl)
                    .setDirectory(new File(clonePath))
                    .call();
        } catch (Exception e) {
            return Git.open(new File(clonePath));
        }
    }

    /**
     * Checkouts to specified commitId (SHA)
     *
     * @param git      a git object (used to clone the repo)
     * @param commitId the SHA we are checking out to
     */
    private static void checkout(Git git, String commitId) throws GitAPIException {
        git.checkout().setName(commitId).call();
    }

    private static PrincipalResponseEntity[] getResponseEntities() {
        HttpResponse<JsonNode> httpResponse;
        Unirest.setTimeouts(0, 0);
        try {
            httpResponse = Unirest.get("http://195.251.210.147:8989/api/sdk4ed/internal/longest-path/with-commit-changes?url=" + GIT_SERVICE_URL + OWNER + "/" + REPOSITORY).asJson();
            return new Gson().fromJson(httpResponse.getBody().toString(), PrincipalResponseEntity[].class);
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get Metrics from Metrics Calculator for every java file (initial calculation)
     *
     * @param projectPath the project root
     */
    private static void setMetrics(String projectPath) {
        MetricsCalculator.start(projectPath);
        String st = MetricsCalculator.printResults();
        MetricsCalculator.reset();
        try {
            String[] s = st.split("\\r?\\n");
            for (int i = 1; i < s.length; ++i) {
                String[] column = s[i].split(";");
                String filePath = column[0] + ".java";
                JavaFile jf = new JavaFile(filePath);
                registerMetrics(column, jf);
                Globals.addJavaFile(jf);
            }
        } catch (Exception ignored) {
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
                MetricsCalculator.start(projectPath, jf.getPath().replace("\\", "/"));
                String st = MetricsCalculator.printResults();
                MetricsCalculator.reset();
                String[] s = st.split("\\r?\\n");
                String[] column = s[1].split(";");
                registerMetrics(column, jf);
                Globals.addJavaFile(jf);
            }
            jfs.forEach(JavaFile::calculateInterest);
        } catch (Exception ignored) {
        }
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
        jf.getQualityMetrics().setClassesNum(Integer.parseInt(calcEntries[12]));
    }
}
