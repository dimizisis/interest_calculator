package data;

import infrastructure.interest.JavaFile;
import org.eclipse.jgit.api.Git;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Globals {

    private static final Set<JavaFile> javaFiles;
    private static final GitHistoryRefactoringMiner miner;
    private static Git git;
    private static boolean hasRefactoring;

    static {
        javaFiles = ConcurrentHashMap.newKeySet();
        miner = new GitHistoryRefactoringMinerImpl();
        hasRefactoring = false;
    }

    public static void addJavaFile(JavaFile jf) {
        if(!getJavaFiles().add(jf)) {
            getJavaFiles().remove(jf);
            getJavaFiles().add(jf);
        }
    }

    public static boolean getHasRefactoring() { return hasRefactoring; }

    public static void setHasRefactoring(boolean b) { hasRefactoring = b; }

    public static void resetHasRefactoring() { hasRefactoring = false; }

    public static Set<JavaFile> getJavaFiles() {
        return javaFiles;
    }

    public static GitHistoryRefactoringMiner getMiner() { return miner; }

    public static Git getGit() {
        return git;
    }

    public static void setGit(Git git) {
        Globals.git = git;
    }
}
