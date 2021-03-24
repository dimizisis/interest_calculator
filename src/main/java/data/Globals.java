package data;

import infrastructure.interest.JavaFile;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Globals {

    private static final Set<JavaFile> javaFiles;
    private static final AtomicInteger revisions;

    static {
        javaFiles = ConcurrentHashMap.newKeySet();
        revisions = new AtomicInteger(1);
    }

    public static Set<JavaFile> getJavaFiles() {
        return javaFiles;
    }

    public static void addJavaFile(JavaFile jf) {
        javaFiles.add(jf);
    }

    public static void addJavaFiles(Set<JavaFile> jfs) {
        javaFiles.addAll(jfs);
    }

    public static Integer getRevisions() {
        return revisions.get();
    }

    public static void incrementRevisions() {
        revisions.incrementAndGet();
    }
}
