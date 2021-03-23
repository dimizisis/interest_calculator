package data;

import infrastructure.interest.JavaFile;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Globals {

    private static Set<JavaFile> javaFiles;
    static {
        javaFiles = ConcurrentHashMap.newKeySet();
    }
    public static Set<JavaFile> getJavaFiles() { return javaFiles; }
    public static void setJavaFiles(Set<JavaFile> javaFiles) {
        Globals.javaFiles = javaFiles;
    }
}
