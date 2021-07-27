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
    private static String projectOwner;
    private static String projectRepo;
    private static String projectPath;
    private static Git git;
    private static StringBuilder output;
    private static String[] outputHeaders;

    static {
        javaFiles = ConcurrentHashMap.newKeySet();
        revisions = new AtomicInteger(1);
        currentSha = "";
        projectURL = "";
        projectOwner = "";
        projectRepo = "";
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
                "RFC\t",
                "LOC\t",
                "SIZE2\t",
                "WMC\t",
                "NOM\t",
                "CBO\t",
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
                + javaFile.getQualityMetrics().getRFC() + "\t"
                + javaFile.getQualityMetrics().getSIZE1() + "\t"
                + javaFile.getQualityMetrics().getSIZE2() + "\t"
                + javaFile.getQualityMetrics().getWMC() + "\t"
                + javaFile.getQualityMetrics().getNOM() + "\t"
                + javaFile.getQualityMetrics().getCBO() + "\t"
                + javaFile.getInterestInEuros() + "\t"
                + javaFile.getKappaValue() + "\n";
        output.append(s);
    }

    public synchronized static void compound() {

        int filesCount = getJavaFiles().size();
        if (filesCount <= 0)
            return;

        double avgCbo = 0.0;
        int totalClassesNum = 0;
        double avgComplexity = 0.0;
        int totalDAC = 0;
        int maxDIT = -1;
        double avgLCOM = 0.0;
        double avgMPC = 0.0;
        int avgNOCC = 0;
        double avgRFC = 0.0;
        int sumLOC = 0;
        int sumSize2 = 0;
        double sumWMC = 0.0;
        double sumNOM = 0.0;
        double sumInterest = 0.0;

        for (JavaFile javaFile : javaFiles) {
            totalClassesNum += javaFile.getQualityMetrics().getClassesNum();
            avgComplexity += javaFile.getQualityMetrics().getComplexity();
            totalDAC += javaFile.getQualityMetrics().getDAC();
            if (javaFile.getQualityMetrics().getDIT() > maxDIT)
                maxDIT = javaFile.getQualityMetrics().getDIT();
            if (javaFile.getQualityMetrics().getLCOM() >= 0.0)
                avgLCOM += javaFile.getQualityMetrics().getLCOM();
            avgMPC += javaFile.getQualityMetrics().getMPC();
            avgNOCC += javaFile.getQualityMetrics().getNOCC();
            avgRFC += javaFile.getQualityMetrics().getRFC();
            sumLOC += javaFile.getQualityMetrics().getSIZE1();
            sumSize2 += javaFile.getQualityMetrics().getSIZE2();
            sumWMC += javaFile.getQualityMetrics().getWMC();
            sumNOM += javaFile.getQualityMetrics().getNOM();
            avgCbo += javaFile.getQualityMetrics().getCBO();
            sumInterest += javaFile.getInterestInEuros();
        }
        output.append(getCurrentSha()).append("\t").append(getRevisionCount()).append("\t").append("Compound\t").append(totalClassesNum).append("\t").append(avgComplexity / filesCount).append("\t").append(totalDAC).append("\t").append(maxDIT).append("\t").append(avgLCOM / filesCount).append("\t").append(avgMPC / filesCount).append("\t").append(avgNOCC / filesCount).append("\t").append(avgRFC / filesCount).append("\t").append(sumLOC).append("\t").append(sumSize2).append("\t").append(sumWMC).append("\t").append(sumNOM).append("\t").append(avgCbo / filesCount).append("\t").append(sumInterest).append("\t").append("-").append("\n");
    }

    private static String preprocessURL() {
        String newURL = projectURL;
        if (newURL.endsWith(".git/"))
            newURL = newURL.replace(".git/", "");
        if (newURL.endsWith(".git"))
            newURL = newURL.replace(".git", "");
        if (newURL.endsWith("/"))
            newURL = newURL.substring(0, newURL.length() - 1);
        return newURL;
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

    public static void setRevisionCount(Integer revision) {
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

    public static String getProjectOwner() {
        return projectOwner;
    }

    public static void setProjectOwner() {
        String newURL = preprocessURL();
        String[] urlSplit = newURL.split("/");
        projectOwner = urlSplit[urlSplit.length - 2].replaceAll(".*@.*:", "");
    }

    public static String getProjectRepo() {
        return projectRepo;
    }

    public static void setProjectRepo() {
        String newURL = preprocessURL();
        String[] urlSplit = newURL.split("/");
        projectRepo = urlSplit[urlSplit.length - 1];
    }
}
