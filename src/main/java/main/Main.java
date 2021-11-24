package main;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import data.Globals;
import db.DatabaseConnection;
import db.InsertToDB;
import db.RetrieveFromDB;
import infrastructure.Project;
import infrastructure.Revision;
import infrastructure.interest.JavaFile;
import infrastructure.newcode.DiffEntry;
import infrastructure.newcode.PrincipalResponseEntity;
import metricsCalculator.calculator.MetricsCalculator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;

import static db.RetrieveFromDB.*;

public class Main {

    /*
    0: Analysis ok
    -1: Git error, wrong url or internet connection problem
    -2: Mandatory arguments not provided
     */

    public static void main(String[] args) throws Exception {

        if (args.length < 5)
            System.exit(-2);

        DatabaseConnection.setDatabaseDriver(args[1]);
        DatabaseConnection.setDatabaseUrl(args[2]);
        DatabaseConnection.setDatabaseUsername(args[3]);
        DatabaseConnection.setDatabasePassword(args[4]);

        Project project = new Project(args[0]);

        try {
            deleteSourceCode(new File(project.getClonePath()));
        } catch (Exception ignored) {
        }

        System.out.printf("Cloning %s...\n", project.getUrl());
        Git git = cloneRepository(project);

        System.out.println("Receiving all commit ids...");
        List<String> diffCommitIds = new ArrayList<>();
        List<String> commitIds = getCommitIds(git);
        if (commitIds.isEmpty())
            return;
        Collections.reverse(commitIds);
        int start = 0;
        boolean existsInDb = false;
        Revision currentRevision = new Revision("", 0);
        try {
            existsInDb = RetrieveFromDB.ProjectExistsInDatabase(project);
            if (existsInDb) {
                List<String> existingCommitIds = getExistingCommitIds(project);
                diffCommitIds = findDifferenceInCommitIds(commitIds, existingCommitIds);
                if (!diffCommitIds.isEmpty())
                    currentRevision = getLastRevision(project);
                else
                    System.exit(0);
            }
        } catch (Exception ignored) {
        }

        if (Objects.isNull(git))
            System.exit(-1);

        if (!existsInDb || diffCommitIds.containsAll(commitIds)) {
            start = 1;
            Objects.requireNonNull(currentRevision).setSha(Objects.requireNonNull(commitIds.get(0)));
            Objects.requireNonNull(currentRevision).setRevisionCount(currentRevision.getRevisionCount() + 1);
            checkout(project, currentRevision, Objects.requireNonNull(git));
            System.out.printf("Calculating metrics for commit %s (%d)...\n", currentRevision.getSha(), currentRevision.getRevisionCount());
            setMetrics(project, currentRevision);
            System.out.println("Calculated metrics for all files from first commit!");
            InsertToDB.insertProjectToDatabase(project);
            insertData(project, currentRevision);
            DatabaseConnection.getConnection().commit();
        } else {
            retrieveJavaFiles(project);
            commitIds = new ArrayList<>(diffCommitIds);
        }

        for (int i = start; i < commitIds.size(); ++i) {
            Objects.requireNonNull(currentRevision).setSha(commitIds.get(i));
            currentRevision.setRevisionCount(currentRevision.getRevisionCount() + 1);
            checkout(Objects.requireNonNull(project), Objects.requireNonNull(currentRevision), Objects.requireNonNull(git));
            System.out.printf("Calculating metrics for commit %s (%d)...\n", currentRevision.getSha(), currentRevision.getRevisionCount());
            try {
                PrincipalResponseEntity[] responseEntities = getResponseEntitiesAtCommit(git, currentRevision.getSha());
                if (Objects.isNull(responseEntities) || responseEntities.length == 0) {
                    if (Globals.getJavaFiles().isEmpty())
                        InsertToDB.insertEmpty(project, currentRevision);
                    else
                        insertData(project, currentRevision);
                    System.out.println("Calculated metrics for all files!");
                    continue;
                }
                System.out.println("Analyzing new/modified commit files...");
                setMetrics(project, currentRevision, responseEntities[0].getDiffEntries());
                System.out.println("Calculated metrics for all files!");
                insertData(project, currentRevision);
            } catch (Exception ignored) {
            }
            DatabaseConnection.getConnection().commit();
        }
        DatabaseConnection.closeConnection(true);
        System.out.printf("Finished analysing %d revisions.\n", Objects.requireNonNull(currentRevision).getRevisionCount());
    }

    /**
     * Inserts the data of the first revision (in list).
     *
     * @param project         the project we are referring to
     * @param currentRevision the current revision we are analysing
     */
    private static void insertData(Project project, Revision currentRevision) {
        if (Globals.getJavaFiles().size() == 0)
            InsertToDB.insertEmpty(project, currentRevision);
        else {
            Globals.getJavaFiles().forEach(jf -> InsertToDB.insertFileToDatabase(project, jf, currentRevision));
            Globals.getJavaFiles().forEach(jf -> InsertToDB.insertMetricsToDatabase(project, jf, currentRevision));
        }
    }

    /**
     * Performs a set subtraction between the received commits and the existing ones (in database).
     *
     * @param receivedCommitIds the list containing the received commits
     * @param existingCommitIds the list containing the existing commits
     * @return the result of the subtraction
     */
    private static List<String> findDifferenceInCommitIds(List<String> receivedCommitIds, List<String> existingCommitIds) {
        List<String> diffCommitIds = new ArrayList<>(receivedCommitIds);
        if (Objects.nonNull(existingCommitIds))
            diffCommitIds.removeAll(existingCommitIds);
        return diffCommitIds;
    }

    /**
     * Deletes source code (if exists) before the analysis
     * procedure.
     *
     * @param file the directory that the repository will be cloned
     */
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

    /**
     * Gets all commit ids for a specific git repo.
     *
     * @param git the git object
     */
    private static List<String> getCommitIds(Git git) {
        List<String> commitIds = new ArrayList<>();
        try {
            String treeName = getHeadName(git.getRepository());
            for (RevCommit commit : git.log().add(git.getRepository().resolve(treeName)).call())
                commitIds.add(commit.getName());
        } catch (Exception ignored) {
        }

        return commitIds;
    }

    public static String getHeadName(Repository repo) {
        String result = null;
        try {
            ObjectId id = repo.resolve(Constants.HEAD);
            result = id.getName();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Gets all commit ids for a specific git repo.
     *
     * @param git the git object
     */
    private static PrincipalResponseEntity[] getResponseEntitiesAtCommit(Git git, String sha) {
        RevCommit headCommit;
        try {
            headCommit = git.getRepository().parseCommit(ObjectId.fromString(sha));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        RevCommit diffWith = Objects.requireNonNull(headCommit).getParent(0);
        FileOutputStream stdout = new FileOutputStream(FileDescriptor.out);
        PrincipalResponseEntity[] principalResponseEntities = new PrincipalResponseEntity[1];
        try (DiffFormatter diffFormatter = new DiffFormatter(stdout)) {
            diffFormatter.setRepository(git.getRepository());
            try {
                List<DiffEntry> diffEntries = new ArrayList<>();
                for (org.eclipse.jgit.diff.DiffEntry entry : diffFormatter.scan(headCommit, diffWith)) {
                    diffEntries.add(new DiffEntry(entry.getOldPath(), entry.getNewPath(), entry.getChangeType().toString()));
                }
                principalResponseEntities[0] = new PrincipalResponseEntity(headCommit.getName(), headCommit.getCommitTime(), diffEntries);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return principalResponseEntities;
    }

    /**
     * Removes those files that are marked as 'DELETED' (new code's call)
     *
     * @param diffEntries the modified java files (new, modified, deleted)
     */
    private static Set<JavaFile> removeDeletedFiles(Revision currentRevision, List<DiffEntry> diffEntries) {
        Set<JavaFile> deletedFiles = ConcurrentHashMap.newKeySet();
        diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.getChangeType().equals("DELETE"))
                .forEach(diffEntry -> {
                    deletedFiles.add(new JavaFile(diffEntry.getOldFilePath(), currentRevision));
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
    private static Set<JavaFile> findNewFiles(Revision currentRevision, List<DiffEntry> diffEntries) {
        Set<JavaFile> newFiles = ConcurrentHashMap.newKeySet();
        diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.getChangeType().equals("ADD"))
                .filter(diffEntry -> diffEntry.getNewFilePath().toLowerCase().endsWith(".java"))
                .forEach(diffEntry -> newFiles.add(new JavaFile(diffEntry.getNewFilePath(), currentRevision)));
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
     * @param project the project we are referring to
     * @return a git object
     */
    private static Git cloneRepository(Project project) {
        try {
            return Git.cloneRepository()
                    .setURI(project.getUrl())
                    .setDirectory(new File(project.getClonePath()))
                    .call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checkouts to specified commitId (SHA)
     *
     * @param project         the project we are referring to
     * @param currentRevision the revision we are checking out to
     * @param git             a git object
     */
    private static void checkout(Project project, Revision currentRevision, Git git) throws GitAPIException {
        try {
            git.checkout().setCreateBranch(true).setName("version" + currentRevision.getRevisionCount()).setStartPoint(currentRevision.getSha()).call();
        } catch (CheckoutConflictException e) {
            deleteSourceCode(new File(project.getClonePath()));
            cloneRepository(project);
            git.checkout().setCreateBranch(true).setName("version" + currentRevision.getRevisionCount()).setStartPoint(currentRevision.getSha()).call();
        }
    }

    /**
     * Sets the metrics of new and modified files.
     *
     * @param project         the project we are referring to
     * @param currentRevision the revision we are analysing
     * @param diffEntries     the list containing the diff entries received.
     */
    private static void setMetrics(Project project, Revision currentRevision, List<DiffEntry> diffEntries) {
        removeDeletedFiles(currentRevision, diffEntries);
        Set<JavaFile> newFiles = findNewFiles(currentRevision, Objects.requireNonNull(diffEntries));
        Set<JavaFile> modifiedFiles = findModifiedFiles(Objects.requireNonNull(diffEntries));
        setMetrics(project, currentRevision, newFiles);
        setMetrics(project, currentRevision, modifiedFiles);
    }

    /**
     * Get Metrics from Metrics Calculator for every java file (initial calculation)
     *
     * @param project the project we are referring to
     */
    private static void setMetrics(Project project, Revision currentRevision) {
        int resultCode = MetricsCalculator.start(project.getClonePath());
        if (resultCode == -1)
            return;
        String st = MetricsCalculator.printResults();
        MetricsCalculator.reset();
        String[] s = st.split("\\r?\\n");
        for (int i = 1; i < s.length; ++i) {
            String[] column = s[i].split(";");
            String filePath = column[0];
            String className = column[1];
            JavaFile jf;
            if (Globals.getJavaFiles().stream().noneMatch(javaFile -> javaFile.getPath().equals(filePath.replace("\\", "/")))) {
                jf = new JavaFile(filePath, currentRevision);
                jf.addClassName(className);
                registerMetrics(column, jf);
                Globals.addJavaFile(jf);
            } else {
                jf = getAlreadyDefinedFile(filePath);
                if (Objects.nonNull(jf)) {
                    if (jf.containsClass(className)) {
                        registerMetrics(column, jf);
                    } else {
                        jf.addClassName(className);
                        appendMetrics(column, jf);
                    }
                }
            }
        }
    }

    /**
     * Finds java file by its path
     *
     * @param filePath the file path
     * @return the java file (JavaFile) whose path matches the given one
     */
    private static JavaFile getAlreadyDefinedFile(String filePath) {
        for (JavaFile jf : Globals.getJavaFiles())
            if (jf.getPath().equals(filePath))
                return jf;
        return null;
    }

    /**
     * Get Metrics from Metrics Calculator for specific java files (new or modified)
     *
     * @param project         the project we are referring to
     * @param currentRevision the revision we are analysing
     * @param jfs             the list of java files
     */
    private static void setMetrics(Project project, Revision currentRevision, Set<JavaFile> jfs) {
        if (jfs.isEmpty()) return;
        int resultCode = MetricsCalculator.start(project.getClonePath(), jfs);
        if (resultCode == -1) return;
        String st = MetricsCalculator.printResults();
        MetricsCalculator.reset();
        String[] s = st.split("\\r?\\n");
        try {
            for (int i = 1; i < s.length; ++i) {
                String[] column = s[i].split(";");
                String filePath = column[0];
                String className = column[1];
                JavaFile jf;
                if (Globals.getJavaFiles().stream().noneMatch(javaFile -> javaFile.getPath().equals(filePath.replace("\\", "/")))) {
                    jf = new JavaFile(filePath, currentRevision);
                    jf.addClassName(className);
                    registerMetrics(column, jf);
                    jf.calculateInterest();
                    Globals.addJavaFile(jf);
                } else {
                    jf = getAlreadyDefinedFile(filePath);
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

    /**
     * Register Metrics to specified java file
     *
     * @param calcEntries entries taken from MetricsCalculator's results
     * @param jf          the java file we are registering metrics to
     */
    private static void appendMetrics(String[] calcEntries, JavaFile jf) {
        jf.getQualityMetrics().setWMC(jf.getQualityMetrics().getWMC() + Double.parseDouble(calcEntries[2]));
        jf.getQualityMetrics().setDIT(jf.getQualityMetrics().getDIT() + Integer.parseInt(calcEntries[3]));
        jf.getQualityMetrics().setNOCC(jf.getQualityMetrics().getNOCC() + Integer.parseInt(calcEntries[4]));
        jf.getQualityMetrics().setRFC(jf.getQualityMetrics().getRFC() + Double.parseDouble(calcEntries[5]));
        if (Double.parseDouble(calcEntries[6]) > 0)
            jf.getQualityMetrics().setLCOM(jf.getQualityMetrics().getLCOM() + Double.parseDouble(calcEntries[6]));
        if (Double.parseDouble(calcEntries[7]) > 0)
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
