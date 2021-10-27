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

    static {
        javaFiles = ConcurrentHashMap.newKeySet();
        revisions = new AtomicInteger(1);
        currentSha = "";
        projectURL = "";
        projectOwner = "";
        projectRepo = "";
        projectPath = "";
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
