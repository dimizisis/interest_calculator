package data;

import infrastructure.interest.JavaFile;

import java.util.HashSet;

public final class Globals {

    private static final HashSet<JavaFile> javaFiles;
    static {
        javaFiles = new HashSet<>();
    }

    public static boolean addJavaFile(JavaFile jf) {
        return javaFiles.add(jf);
    }

    public static boolean removeJavaFile(JavaFile jf) {
        return javaFiles.remove(jf);
    }

    public static boolean removeJavaFileByPath(String path) {
        try {
            javaFiles
                    .stream()
                    .filter(javaFile -> javaFile.getPath().equals(path))
                    .forEach(javaFiles::remove);
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static HashSet<JavaFile> getJavaFiles() { return javaFiles; }

}
