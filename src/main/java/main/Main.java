package main;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import static db.RetrieveFromDB.*;

public class Main {

    /*
    0: Analysis ok
    -1: Git error, wrong url or internet connection problem
    -2: Mandatory arguments not provided
     */

    public static void main(String[] args) throws Exception {

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.OFF);

        if (args.length < 6)
            System.exit(-2);

        DatabaseConnection.setDatabaseDriver(args[1]);
        DatabaseConnection.setDatabaseUrl(args[2]);
        DatabaseConnection.setDatabaseUsername(args[3]);
        DatabaseConnection.setDatabasePassword(args[4]);

        Project project = new Project(args[0], args[5]);

        try {
            deleteSourceCode(new File(project.getClonePath()));
        } catch (Exception ignored) {
        }

        System.out.printf("Cloning %s...\n", project.getUrl());
        Git git = cloneRepository(project);
        Globals.setGit(git);

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
                    insertData(project, currentRevision);
                    System.out.println("Calculated metrics for all files!");
                    continue;
                }
                System.out.println("Analyzing new/modified commit files...");
                setMetrics(project, currentRevision, responseEntities[0]);
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
        if (Globals.getJavaFiles().isEmpty())
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
                Set<DiffEntry> addDiffEntries = new HashSet<>();
                Set<DiffEntry> modifyDiffEntries = new HashSet<>();
                Set<DiffEntry> renameDiffEntries = new HashSet<>();
                Set<DiffEntry> deleteDiffEntries = new HashSet<>();
                for (org.eclipse.jgit.diff.DiffEntry entry : diffFormatter.scan(diffWith, headCommit)) {
                    if (entry.getChangeType().equals(org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD) || entry.getChangeType().equals(org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY) && entry.getNewPath().toLowerCase().endsWith(".java"))
                        addDiffEntries.add(new DiffEntry(entry.getOldPath(), entry.getNewPath(), entry.getChangeType().toString()));
                    else if (entry.getChangeType().equals(org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY) && entry.getNewPath().toLowerCase().endsWith(".java"))
                        modifyDiffEntries.add(new DiffEntry(entry.getOldPath(), entry.getNewPath(), entry.getChangeType().toString()));
                    else if (entry.getChangeType().equals(org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME) && entry.getNewPath().toLowerCase().endsWith(".java"))
                        renameDiffEntries.add(new DiffEntry(entry.getOldPath(), entry.getNewPath(), entry.getChangeType().toString()));
                    else if (entry.getChangeType().equals(org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE) && entry.getNewPath().toLowerCase().endsWith(".java"))
                        deleteDiffEntries.add(new DiffEntry(entry.getOldPath(), entry.getNewPath(), entry.getChangeType().toString()));
                }
                principalResponseEntities[0] = new PrincipalResponseEntity(headCommit.getName(), headCommit.getCommitTime(), addDiffEntries, modifyDiffEntries, renameDiffEntries, deleteDiffEntries);
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
    private static Set<JavaFile> removeDeletedFiles(Revision currentRevision, Set<DiffEntry> diffEntries) {
        Set<JavaFile> deletedFiles = ConcurrentHashMap.newKeySet();
        diffEntries
                .parallelStream()
                .forEach(diffEntry -> {
                    deletedFiles.add(new JavaFile(diffEntry.getOldFilePath(), currentRevision));
                    Globals.getJavaFiles().removeIf(javaFile -> javaFile.getPath().endsWith(diffEntry.getOldFilePath()));
                });
        return deletedFiles;
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
     * @param entity          the entity with the list containing the diff entries received.
     */
    private static void setMetrics(Project project, Revision currentRevision, PrincipalResponseEntity entity) {
        if (!entity.getDeleteDiffEntries().isEmpty())
            removeDeletedFiles(currentRevision, entity.getDeleteDiffEntries());
        if (!entity.getAddDiffEntries().isEmpty())
            setMetrics(project, currentRevision, entity.getAddDiffEntries().stream().map(diffEntry -> new JavaFile(diffEntry.getNewFilePath(), currentRevision)).collect(Collectors.toSet()));
        if (!entity.getModifyDiffEntries().isEmpty())
            setMetrics(project, currentRevision, entity.getModifyDiffEntries().stream().map(diffEntry -> new JavaFile(diffEntry.getNewFilePath(), currentRevision)).collect(Collectors.toSet()));
        if (!entity.getRenameDiffEntries().isEmpty())
            entity.getRenameDiffEntries()
                    .forEach(diffEntry -> {
                        for (JavaFile javaFile : Globals.getJavaFiles()) {
                            if (javaFile.getPath().endsWith(diffEntry.getOldFilePath()))
                                javaFile.setPath(javaFile.getPath().replace(diffEntry.getOldFilePath(), diffEntry.getNewFilePath()));
                        }
                    });
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
                    Globals.addJavaFile(jf);
                } else {
                    jf = getAlreadyDefinedFile(filePath);
                    if (Objects.nonNull(jf)) {
                        if (jf.containsClass(className)) {
                            appendMetrics(column, jf);
                        } else {
                            jf.addClassName(className);
                            appendMetrics(column, jf);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
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
        if (jfs.isEmpty())
            return;
        int resultCode = MetricsCalculator.start(project.getClonePath(), jfs);
        if (resultCode == -1)
            return;
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
                            appendMetrics(column, jf, currentRevision);
                        } else if (!jf.containsClass(className)) {
                            jf.addClassName(className);
                            appendMetrics(column, jf, currentRevision);
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
        jf.setOldQualityMetrics(jf.getQualityMetrics());
    }

    /**
     * Append Metrics to specified java file
     *
     * @param calcEntries entries taken from MetricsCalculator's results
     * @param jf          the java file we are registering metrics to
     */
    private static void appendMetrics(String[] calcEntries, JavaFile jf, Revision revision) {
        jf.setOldQualityMetrics(jf.getQualityMetrics());
        if (!jf.getQualityMetrics().getRevision().equals(revision)) {
            jf.getQualityMetrics().zero();
            jf.getQualityMetrics().setRevision(new Revision(revision.getSha(), revision.getRevisionCount()));
        } else {
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

    /**
     * Append Metrics to specified java file
     *
     * @param calcEntries entries taken from MetricsCalculator's results
     * @param jf          the java file we are registering metrics to
     */
    private static void appendMetrics(String[] calcEntries, JavaFile jf) {
        jf.setOldQualityMetrics(jf.getQualityMetrics());
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
