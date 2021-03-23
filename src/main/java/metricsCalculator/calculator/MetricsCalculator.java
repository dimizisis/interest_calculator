package metricsCalculator.calculator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import metricsCalculator.containers.ClassMetricsContainer;
import metricsCalculator.containers.PackageMetricsContainer;
import metricsCalculator.containers.ProjectMetricsContainer;
import metricsCalculator.metrics.ProjectMetrics;
import metricsCalculator.output.PrintResults;
import metricsCalculator.visitors.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class MetricsCalculator {

    private static final ClassMetricsContainer classMetricsContainer = new ClassMetricsContainer();
    private static final PackageMetricsContainer packageMetricsContainer = new PackageMetricsContainer();
    private static final ProjectMetricsContainer projectMetricsContainer = new ProjectMetricsContainer();
    private static final Set<String> classesToAnalyse = new HashSet<>();
    private static String currentProject;

    /**
     * Start the whole process
     *
     * @return 0 if everything went ok, -1 otherwise
     *
     */
    public static int start(String projectDir) {
        currentProject = projectDir;
        ProjectRoot projectRoot = getProjectRoot();
        List<SourceRoot> sourceRoots = projectRoot.getSourceRoots();
        try { createSymbolSolver(); } catch (IllegalStateException e){ return -1; }
        if (createClassSet(sourceRoots) == 0) {
            System.err.println("No classes could be identified! Exiting...");
            System.exit(-1);
        }
        startCalculations(sourceRoots);
        calculateAllMetrics(getCurrentProject());
        return 0;
    }

    /**
     * Start the whole process
     *
     * @return 0 if everything went ok, -1 otherwise
     */
    public static int start(String projectDir, String filePath) {
        currentProject = projectDir;
        ProjectRoot projectRoot = getProjectRoot();
        List<SourceRoot> sourceRoots = projectRoot.getSourceRoots();
        try {
            createSymbolSolver();
        } catch (IllegalStateException e) {
            return -1;
        }
        if (createClassSet(sourceRoots) == 0) {
            System.err.println("No classes could be identified! Exiting...");
            System.exit(-1);
        }
        startCalculations(sourceRoots, filePath);
        calculateAllMetrics(getCurrentProject());
        return 0;
    }

    /**
     * Get the project root
     */
    private static ProjectRoot getProjectRoot() {
//        System.out.println("Collecting source roots...");
        return new SymbolSolverCollectionStrategy()
                .collect(Paths.get(getCurrentProject()));
    }

    /**
     * Create the symbol solver
     * that will be used to identify
     * user-defined classes
     */
    private static void createSymbolSolver() {
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(getCurrentProject()));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(javaParserTypeSolver);
        StaticJavaParser
                .getConfiguration()
                .setSymbolResolver(symbolSolver);
    }

    /**
     * Creates the class set (add appropriate classes)
     */
    private static int createClassSet(List<SourceRoot> sourceRoots) {
        try {
            sourceRoots
                    .forEach(sourceRoot -> {
                        try {
                            sourceRoot.tryToParse()
                                    .stream()
                                    .filter(res -> res.getResult().isPresent())
                                    .forEach(res -> addToClassSet(res.getResult().get()));
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
        return classesToAnalyse.size();
    }

    /**
     * Adds a valid class to class set
     *
     * @param cu the compilation unit of class provided
     */
    private static void addToClassSet(CompilationUnit cu) {
        try {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> classesToAnalyse.add(c.resolve().getQualifiedName()));
        } catch (Exception ignored) {
        }
        try {
            cu.findAll(EnumDeclaration.class).forEach(en -> classesToAnalyse.add(en.resolve().getQualifiedName()));
        } catch (Exception ignored) {
        }
    }

    /**
     * Starts the calculations
     *
     * @param sourceRoots the list of source roots of project
     */
    private static void startCalculations(List<SourceRoot> sourceRoots, String filePath) {
        sourceRoots.forEach(sourceRoot -> {
            try {
                sourceRoot.tryToParse().forEach(res -> {
                    if (res.getResult().isPresent()) {
                        CompilationUnit cu = res.getResult().get();
                        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
                            if (cu.getStorage().isPresent() && cu.getStorage().get().getPath().toString().replace("\\", "/").equals(filePath))
                                analyzeCompilationUnit(cu, sourceRoot.getRoot().toString().replace("\\", "/"));
                        });
                    }
                });
            } catch (IOException ignored) {
            }
        });
    }
    private static void startCalculations(List<SourceRoot> sourceRoots) {
        sourceRoots
                .forEach(sourceRoot -> {
                    try {
                        sourceRoot.tryToParse()
                                .stream()
                                .filter(res -> res.getResult().isPresent())
                                .forEach(res -> analyzeCompilationUnit(res.getResult().get(), sourceRoot.getRoot().toString().replace("\\", "/")));
                    } catch (Exception ignored) {
                    }
                });
    }

    /**
     * Calculates all metrics (aggregated) after class by
     * class visit is over
     *
     * @param project the name of the project we are referring to
     */
    private static void calculateAllMetrics(String project) {
        getProjectMetricsContainer().getMetrics(project).calculateAllMetrics(project);
    }

    /**
     * Analyzes the compilation unit given.
     *
     * @param cu         the compilation unit given
     * @param sourceRoot the compilation unit's source root
     */
    private static void analyzeCompilationUnit(CompilationUnit cu, String sourceRoot) {
        analyzeClassOrInterfaces(cu, sourceRoot);
        analyzeEnums(cu, sourceRoot);
    }

    /**
     * Analyzes the classes (or interfaces) given a compilation unit.
     *
     * @param cu         the compilation unit given
     * @param sourceRoot the compilation unit's source root
     */
    private static void analyzeClassOrInterfaces(CompilationUnit cu, String sourceRoot) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
            try {
                c.accept(new ClassVisitor(c, cu, sourceRoot, getClassMetricsContainer()), null);
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Analyzes the enumerations given a compilation unit.
     *
     * @param cu         the compilation unit given
     * @param sourceRoot the compilation unit's source root
     */
    private static void analyzeEnums(CompilationUnit cu, String sourceRoot) {
        cu.findAll(EnumDeclaration.class).forEach(c -> {
            try {
                c.accept(new ClassVisitor(c, cu, sourceRoot, getClassMetricsContainer()), null);
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Prints results after the whole process is done
     */
    public static String printResults() {
        PrintResults handler = new PrintResults();
        Map<?, ?> projects = getProjectMetricsContainer().getProjects();
        Set<?> projectSet = projects.entrySet();
        projectSet.forEach(o -> {
            Map.Entry<?, ?> currentProject = (Map.Entry<?, ?>) o;
            handler.handleProject((String) currentProject.getKey(), (ProjectMetrics) currentProject.getValue());
        });
        return handler.getOutput();
    }

    public static ClassMetricsContainer getClassMetricsContainer() {
        return classMetricsContainer;
    }

    public static PackageMetricsContainer getPackageMetricsContainer() {
        return packageMetricsContainer;
    }

    public static ProjectMetricsContainer getProjectMetricsContainer() {
        return projectMetricsContainer;
    }

    public static String getCurrentProject() {
        return currentProject;
    }

    public static boolean withinAnalysisBounds(String className) {
        return classesToAnalyse.contains(className);
    }
}
