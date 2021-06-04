package data;

import infrastructure.interest.JavaFile;
import org.eclipse.jgit.api.Git;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Globals {

    private static final Set<JavaFile> javaFiles;
    private static final AtomicInteger revisions;
    private static String currentSha;
    private static String projectURL;
    private static String projectPath;
    private static Git git;
    private static StringBuilder output;
    private static String[] outputHeaders;

    static {
        javaFiles = ConcurrentHashMap.newKeySet();
        revisions = new AtomicInteger(1);
        currentSha = "";
        projectURL = "";
        projectPath = "";
        outputHeaders = new String[]{
                "CommitId\t",
                "RevisionCount\t",
                "InvolvedFile\t",
                "ClassesNum\t",
                "Complexity\t",
                "DAC\t",
                "DIT\t",
                "LCOM\t",
                "MPC\t",
                "NOCC\t",
                "OldLOC\t",
                "RFC\t",
                "LOC\t",
                "SIZE2\t",
                "WMC\t",
                "NOM\t",
                "InterestEuros\t",
                "Kappa\n"
        };
        output = new StringBuilder();
    }

    public static Set<JavaFile> getJavaFiles() {
        return javaFiles;
    }

    public static void addJavaFile(JavaFile jf) {
        if(!getJavaFiles().add(jf)) {
            getJavaFiles().remove(jf);
            getJavaFiles().add(jf);
        }
    }

    public synchronized static void append(JavaFile javaFile) {
        String s = getCurrentSha() + "\t"
                + getRevisionCount() + "\t"
                + javaFile.getPath() + "\t"
                + javaFile.getQualityMetrics().getClassesNum() + "\t"
                + javaFile.getQualityMetrics().getComplexity() + "\t"
                + javaFile.getQualityMetrics().getDAC() + "\t"
                + javaFile.getQualityMetrics().getDIT() + "\t"
                + javaFile.getQualityMetrics().getLCOM() + "\t"
                + javaFile.getQualityMetrics().getMPC() + "\t"
                + javaFile.getQualityMetrics().getNOCC() + "\t"
                + javaFile.getQualityMetrics().getOldSIZE1() + "\t"
                + javaFile.getQualityMetrics().getRFC() + "\t"
                + javaFile.getQualityMetrics().getSIZE1() + "\t"
                + javaFile.getQualityMetrics().getSIZE2() + "\t"
                + javaFile.getQualityMetrics().getWMC() + "\t"
                + javaFile.getQualityMetrics().getNOM() + "\t"
                + javaFile.getInterestInEuros() + "\t"
                + javaFile.getKappaValue() + "\n";
        output.append(s);
    }

    public static String getProjectURL() {
        return projectURL;
    }

    public static String getProjectPath() {
        return projectPath;
    }

    public static Git getGit() {
        return git;
    }

    public static Integer getRevisionCount() {
        return getRevisions().get();
    }

    public static AtomicInteger getRevisions() {
        return revisions;
    }

    public static void setRevision(Integer revision) {
        revisions.set(revision);
    }

    public synchronized static void setCurrentSha(String sha) {
        currentSha = sha;
    }

    public synchronized static String getCurrentSha() {
        return currentSha;
    }

    public static void setProjectURL(String url) {
        projectURL = url;
    }
    public static void setProjectPath(String path) {
        projectPath = path;
    }

    public static void setGit(Git newGit) {
        git = newGit;
    }

    public static StringBuilder getOutput() {
        return output;
    }

    public static String[] getOutputHeaders() {
        return outputHeaders;
    }
}
