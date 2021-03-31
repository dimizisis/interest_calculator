package data;

import infrastructure.interest.JavaFile;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Globals {

    private static final Set<JavaFile> javaFiles;
    private static final AtomicInteger revisions;
    private static String currentSha;
    private static String projectURL;

    static {
        javaFiles = ConcurrentHashMap.newKeySet();
        revisions = new AtomicInteger(1);
        currentSha = "";
        projectURL = "";
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

    public static Integer getRevisions() {
        return revisions.get();
    }

    public static void incrementRevisions() {
        revisions.incrementAndGet();
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
}
