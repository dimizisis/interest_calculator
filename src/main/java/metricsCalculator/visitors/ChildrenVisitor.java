package metricsCalculator.visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import data.Globals;
import infrastructure.interest.JavaFile;
import metricsCalculator.calculator.MetricsCalculator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChildrenVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(ClassOrInterfaceDeclaration javaClass, Void arg) {

        List<String> superClassNames = getSuperClassNames(javaClass);

        if (Objects.nonNull(superClassNames)) {

            JavaFile jf = findAncestorFile(superClassNames);

            if (Objects.nonNull(jf)) {
                for (String superClassName : superClassNames) {
                    if (this.withinAnalysisBounds(superClassName)) {
                        if (javaClass.getFullyQualifiedName().isPresent())
                            jf.getQualityMetrics().addClassChildren(javaClass.getFullyQualifiedName().get());
                    }
                }
            }
        }
    }

    /**
     * Find ancestor java file
     *
     * @param superClassNames the class names of ancestors
     * @return java file corresponding to ancestor
     */
    private JavaFile findAncestorFile(List<String> superClassNames) {
        for (String superClassName : superClassNames)
            for (JavaFile javaFile : Globals.getJavaFiles())
                if (javaFile.containsClass(superClassName))
                    return javaFile;
        return null;
    }

    /**
     * Get superclass name of class we are referring to
     *
     * @param javaClass class or enum we are refering to
     * @return superclasses names
     */
    private List<String> getSuperClassNames(ClassOrInterfaceDeclaration javaClass) {
        try {
            return javaClass
                    .getExtendedTypes()
                    .stream()
                    .map(extendedType -> {
                        try {
                            return extendedType.resolve().getQualifiedName();
                        } catch (UnsolvedSymbolException e) {
                            return null;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if the class with the given class name
     * is within analysis bounds (is user-defined)
     *
     * @param className java class name given
     * @return true if class is within analysis bounds,
     * false otherwise
     */
    private boolean withinAnalysisBounds(String className) {
        return MetricsCalculator.withinAnalysisBounds(className);
    }
}
