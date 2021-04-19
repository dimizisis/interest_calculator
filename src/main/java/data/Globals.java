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

    static {
        javaFiles = ConcurrentHashMap.newKeySet();
        revisions = new AtomicInteger(1);
        currentSha = "";
        projectURL = "";
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

}
