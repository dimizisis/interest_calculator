package metricsCalculator.visitors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import metricsCalculator.calculator.MetricsCalculator;
import metricsCalculator.containers.ClassMetricsContainer;
import metricsCalculator.metrics.ClassMetrics;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class ClassVisitor extends VoidVisitorAdapter<Void> {

    private String myFile;
    private String myClassName;
    private ClassMetricsContainer classMetricsContainer;
    private ClassMetrics classMetrics;
    private final Set<String> efferentCoupledClasses = new HashSet<>();
    private String srcRoot;

    private final Set<String> responseSet = new HashSet<>();
    private final List<String> methodsCalled = new ArrayList<>();

    private final List<TreeSet<String>> methodIntersection = new ArrayList<>();
    private CompilationUnit compilationUnit;

    public ClassVisitor(TypeDeclaration<?> jc, CompilationUnit cu, String srcRoot, ClassMetricsContainer classMap) {
        if (!jc.isTopLevelType())
            return;
        this.classMetricsContainer = classMap;
        this.compilationUnit = cu;
        try {
            this.myFile = cu.getStorage().get().getSourceRoot().toString().replace("\\", "/").replace(MetricsCalculator.getProjectRoot().getRoot().toString().replace("\\", "/"), "").substring(1) + "/" + jc.resolve().getQualifiedName().replace(".", "/");
            this.myClassName = jc.resolve().getQualifiedName();
        } catch (Exception e) {
            return;
        }
        this.classMetrics = this.classMetricsContainer
                .getMetrics(this.myClassName);
        this.srcRoot = srcRoot;
    }

    @Override
    public void visit(EnumDeclaration en, Void arg) {
        super.visit(en, arg);

        String packageName = getPackageName(en);

        if (packageName == null) return;

        MetricsCalculator.getPackageMetricsContainer().addClassToPackage(packageName, this.myClassName, this.classMetrics);
        MetricsCalculator.getPackageMetricsContainer().addPackage(packageName);

        this.classMetrics.setVisited();
        if (en.isPublic())
            this.classMetrics.setPublic();

        visitAllClassMethods(en);

        calculateMetrics(en);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration javaClass, Void arg) {
        super.visit(javaClass, arg);

        String packageName = getPackageName(javaClass);

        if (packageName == null) return;

        MetricsCalculator.getPackageMetricsContainer().addClassToPackage(packageName, this.myClassName, this.classMetrics);
        MetricsCalculator.getPackageMetricsContainer().addPackage(packageName);

        this.classMetrics.setVisited();
        if (javaClass.isPublic())
            this.classMetrics.setPublic();
        if (javaClass.isAbstract())
            this.classMetrics.setAbstract();

        List<String> superClassNames = getSuperClassNames(javaClass);

        visitAllClassMethods(javaClass);

        if (Objects.nonNull(superClassNames))
            superClassNames
                    .stream()
                    .filter(this::withinAnalysisBounds).forEach(superClassName -> {
                        this.classMetricsContainer.getMetrics(superClassName).incNoc();
                        registerCoupling(superClassName);
            });
        calculateMetrics(javaClass);
    }

    /**
     * Visit all class methods & register metrics values
     *
     * @param javaClass class or enum we are referring to
     */
    private void visitAllClassMethods(TypeDeclaration<?> javaClass) {
        javaClass.getMethods()
                .forEach(method -> visitMethod(javaClass, method));
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
                    .map(extendedType -> extendedType.resolve().getQualifiedName())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get package name of enum we are referring to
     *
     * @param javaClass the class or enum we are referring to
     * @return package name
     */
    private String getPackageName(TypeDeclaration<?> javaClass) {
        try {
            return javaClass.resolve().getPackageName();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Calculate DIT metric value for the class we are referring to
     *
     * @param className  the class we are referring to
     * @param superClass the class we are referring to
     * @return DIT metric value
     */
    private int calculateDit(String className, ResolvedReferenceType superClass) {
        if (className.equals("java.lang.Object"))
            return 0;

        int dit = this.classMetricsContainer.getMetrics(className).getDit();

        if (dit != -1)
            return dit;

        dit = 1;

        if (Objects.nonNull(superClass)) {
            if (withinAnalysisBounds(superClass)) {
                ResolvedReferenceType newSuperClass = null;
                try {
                    newSuperClass = superClass.getAllAncestors().get(superClass.getAllAncestors().size() - 1);
                } catch (UnsolvedSymbolException e) {
                    dit += calculateDit(superClass.getQualifiedName(), null);
                }
                dit += calculateDit(superClass.getQualifiedName(), newSuperClass);
            }
            List<ResolvedReferenceType> interfaces = getValidInterfaces(superClass);
            for (ResolvedReferenceType anInterface : interfaces) {
                int tmpDit = 1;
                if (withinAnalysisBounds(anInterface)) {
                    try {
                        ResolvedReferenceType ancestor = anInterface.getAllAncestors().get(superClass.getAllAncestors().size() - 1);
                        tmpDit += calculateDit(anInterface.getQualifiedName(), ancestor);
                    } catch (Exception ignored) {
                    }
                }

                if (tmpDit > dit)
                    dit = tmpDit;
            }
        }
        this.classMetricsContainer.getMetrics(className).setDit(dit);
        return dit;
    }

    /**
     * Calculate CC (Cyclomatic Complexity) metric value for
     * the class we are referring to
     *
     * @param javaClass the class or enum we are referring to
     * @return CC metric value
     */
    private double calculateWmcCc(TypeDeclaration<?> javaClass) {

        float total_ifs = 0.0f;
        int valid_classes = 0;

        for (MethodDeclaration method : javaClass.getMethods()) {
            int ifs;
            if (!method.isAbstract() && !method.isNative()) {
                ifs = countIfs(method) + countSwitch(method) + 1;
                total_ifs += ifs;
                ++valid_classes;
            }
        }
        if (javaClass.getConstructors().size() == 0)
            ++valid_classes;

        return valid_classes > 0 ? (total_ifs / valid_classes) : -1;
    }

    /**
     * Count how many switch statements there are within a method
     *
     * @param method the method we are referring to
     * @return switch count
     */
    private int countSwitch(MethodDeclaration method) {
        final int[] count = {0};
        method.findAll(SwitchStmt.class).forEach(switchStmt -> count[0] += switchStmt.getEntries().size());
        return count[0];
    }

    /**
     * Count how many if statements there are within a method
     *
     * @param method the method we are referring to
     * @return if count
     */
    private int countIfs(MethodDeclaration method) {
        return method.findAll(IfStmt.class).size();
    }

    /**
     * Calculate Size1 (LOC) metric value for
     * the class we are referring to
     *
     * @param javaClass the class or enum we are referring to
     * @return Size1 metric value
     */
    private int calculateSize1(TypeDeclaration<?> javaClass) {
        int size = 0;
        size += javaClass.getMethods()
                .stream()
                .filter(method -> method.isAbstract() || method.isNative())
                .count();
        size += javaClass.getMethods()
                .stream()
                .filter(method -> method.getBegin().isPresent() && method.getEnd().isPresent())
                .mapToInt(method -> method.getEnd().get().line - method.getBegin().get().line + 1)
                .sum();
        size += javaClass.getFields().size();
        return size;
    }

    /**
     * Calculate Size2 (Fields + Methods size) metric value for
     * the class we are referring to
     *
     * @param javaClass the class or enum we are referring to
     * @return Size2 metric value
     */
    private int calculateSize2(TypeDeclaration<?> javaClass) {
        return javaClass.getFields().size() + javaClass.getMethods().size();
    }

    /**
     * Calculate number of classes contained in the class
     * we are referring to
     *
     * @param javaClass the class or enum we are referring to
     * @return the number of classes contained
     * in the class we are referring to
     */
    private int calculateClassesNum(TypeDeclaration<?> javaClass) {
        int classesNum = 1;
        for (BodyDeclaration<?> member : javaClass.getMembers()) {
            if (member.isClassOrInterfaceDeclaration()) {
                ++classesNum;
            }
        }
        return classesNum;
    }

    /**
     * Calculate DAC metric value for
     * the class we are referring to
     *
     * @param javaClass the class or enum we are referring to
     * @return DAC metric value
     */
    private int calculateDac(TypeDeclaration<?> javaClass) {
        int dac = 0;
        for (FieldDeclaration field : javaClass.getFields()) {
            if (field.getElementType().isPrimitiveType())
                continue;
            String typeName;
            try {
                typeName = field.getElementType().resolve().describe();
            } catch (Exception e) {
                continue;
            }
            if (withinAnalysisBounds(typeName)) {
                CompilationUnit cu = null;
                try {
                    try {
                        String path = srcRoot + "/" + typeName.replace(".", "/") + ".java";
                        cu = StaticJavaParser.parse(new File(path));
                    } catch (FileNotFoundException ignored1) {
                    }
                    try {
                        Optional<ClassOrInterfaceDeclaration> cl = cu.getClassByName(field.getElementType().asString());
                        if (!cl.isPresent())
                            continue;
                        ++dac;
                    } catch (NullPointerException ignored) {
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return dac;
    }

    /**
     * Calculate LCOM metric value for
     * the class we are referring to
     *
     * @return LCOM metric value
     */
    private int calculateLCOM() {
        int lcom = 0;
        for (int i = 0; i < this.methodIntersection.size(); ++i) {
            for (int j = i + 1; j < this.methodIntersection.size(); ++j) {
                AbstractSet<?> intersection = (TreeSet<?>) (this.methodIntersection.get(i)).clone();
                if ((!intersection.isEmpty()) || (!this.methodIntersection.isEmpty())) {
                    intersection.retainAll(this.methodIntersection.get(j));
                    if (intersection.size() == 0)
                        ++lcom;
                    else
                        --lcom;
                }
            }
        }
        return this.methodIntersection.size() == 0 ? -1 : Math.max(lcom, 0);
    }

    /**
     * Visit the method given & register metrics values
     *
     * @param javaClass the class we are referring to
     * @param method    the method of javaClass we are referring to
     */
    public void visitMethod(TypeDeclaration<?> javaClass, MethodDeclaration method) {

        this.methodIntersection.add(new TreeSet<>());
        if (!method.isConstructorDeclaration())
            this.classMetrics.incWmc();

        try {
            registerCoupling(method.resolve().getReturnType().describe());
        } catch (Exception ignored) {
        }

        incRFC(method.resolve().getQualifiedName());
        investigateExceptions(method);
        investigateModifiers(method);
        investigateParameters(method);
        investigateInvocation(method);
        investigateFieldAccess(method, javaClass.getFields());
    }

    /**
     * Register field access of method given
     *
     * @param method      the method we are referring to
     * @param classFields the class fields of class we are
     *                    referring to
     */
    private void investigateFieldAccess(MethodDeclaration method, List<FieldDeclaration> classFields) {
        try {
            method.findAll(NameExpr.class).forEach(expr -> classFields.forEach(classField -> classField.getVariables()
                    .stream().filter(var -> var.getNameAsString().equals(expr.getNameAsString()))
                    .forEach(var -> registerFieldAccess(expr.getNameAsString()))));
        } catch (Exception ignored) {
        }
    }

    /**
     * Register exception usage of method given
     *
     * @param method the method we are referring to
     */
    private void investigateExceptions(MethodDeclaration method) {
        try {
            method.resolve().getSpecifiedExceptions()
                    .forEach(exception -> registerCoupling(exception.describe()));
        } catch (Exception ignored) {
        }
    }

    /**
     * Register modifiers usage of method given
     *
     * @param method the method we are referring to
     */
    private void investigateModifiers(MethodDeclaration method) {
        try {
            method.getModifiers()
                    .stream()
                    .filter(mod -> mod.getKeyword().equals(Modifier.Keyword.PUBLIC))
                    .forEach(mod -> this.classMetrics.incNpm());
        } catch (Exception ignored) {
        }
    }

    /**
     * Register parameters of method given
     *
     * @param method the method we are referring to
     */
    private void investigateParameters(MethodDeclaration method) {
        try {
            method.getParameters()
                    .forEach(p -> registerCoupling(p.getType().resolve().describe()));
        } catch (Exception ignored) {
        }
    }

    /**
     * Register invocation of method given
     *
     * @param method the method we are referring to
     */
    private void investigateInvocation(MethodDeclaration method){
        try {
            try {
                method.findAll(MethodCallExpr.class)
                        .forEach(methodCall -> {
                            ResolvedMethodDeclaration resolvedMethodDeclaration = methodCall.resolve();
                            registerMethodInvocation(resolvedMethodDeclaration.getQualifiedName().substring(0, resolvedMethodDeclaration.getQualifiedName().lastIndexOf(".")), resolvedMethodDeclaration.getQualifiedSignature());
                        });
            } catch (StackOverflowError st) {}
        } catch (Exception ignored){}
    }

    /**
     * Register coupling of java class given
     *
     * @param className class name coupled with
     *                  the class we are referring to
     */
    private void registerCoupling(String className) {
        String simpleClassName = className.contains(".") ? className.substring(className.lastIndexOf('.')+1) : className.substring(className.lastIndexOf('.'));
        String simpleMyClassName = this.myClassName.contains(".") ? this.myClassName.substring(this.myClassName.lastIndexOf('.')+1) : this.myClassName.substring(this.myClassName.lastIndexOf('.'));

        if (this.compilationUnit.getClassByName(simpleClassName).isPresent() && this.compilationUnit.getClassByName(simpleMyClassName).isPresent()) {
            ClassOrInterfaceDeclaration cl = this.compilationUnit.getClassByName(simpleClassName).get();
            ClassOrInterfaceDeclaration myCl = this.compilationUnit.getClassByName(simpleMyClassName).get();

            if ((withinAnalysisBounds(className)) && (!this.myClassName.equals(className))) {
                if (cl.isTopLevelType() && (myCl.isInnerClass() || myCl.isNestedType()))
                    return;
                if (cl.isAncestorOf(myCl) && !cl.isInterface())
                    this.efferentCoupledClasses.add(className);
                this.classMetricsContainer.getMetrics(className).addAfferentCoupling(this.myClassName);
            }
        } else {
            if ((withinAnalysisBounds(className))) {
                if ((!this.myClassName.equals(className))) {
                    this.efferentCoupledClasses.add(className);
                    this.classMetricsContainer.getMetrics(className).addAfferentCoupling(this.myClassName);
                }
            }
        }
    }

    /**
     * Register field access
     *
     * @param fieldName the field we are referring to
     */
    private void registerFieldAccess(String fieldName) {
        registerCoupling(this.myClassName);
        this.methodIntersection.get(this.methodIntersection.size() - 1).add(fieldName);
    }

    /**
     * Register method invocation of class given
     *
     * @param className the name of the class we are referring to
     */
    private void registerMethodInvocation(String className, String signature) {
        registerCoupling(className);
        incRFC(signature);
        incMPC(signature);
    }

    /**
     * Increase MPC metric value with class signature given
     *
     * @param signature the signature of the method we are referring to
     */
    private void incMPC(String signature) {
        this.methodsCalled.add(signature);
    }

    /**
     * Increase RFC metric value with class signature given
     *
     * @param signature the signature of the method we are referring to
     */
    private void incRFC(String signature) {
        this.responseSet.add(signature);
    }

    /**
     * Get valid interfaces (ancestors) of class given
     *
     * @param javaClass the class we are referring to
     * @return list of valid interfaces
     */
    private List<ResolvedReferenceType> getValidInterfaces(ResolvedReferenceType javaClass) {
        List<ResolvedReferenceType> ancestorsIf;
        try {
            ancestorsIf = javaClass.getAllInterfacesAncestors();
        } catch (UnsolvedSymbolException e) {
            return new ArrayList<>();
        }

        List<ResolvedReferenceType> validInterfaces = new ArrayList<>();
        for (ResolvedReferenceType resolvedReferenceType : ancestorsIf) {
            try {
                if (withinAnalysisBounds(resolvedReferenceType.getQualifiedName()))
                    validInterfaces.add(resolvedReferenceType);
            } catch (Exception ignored) {
            }
        }
        return validInterfaces;
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

    /**
     * Check if the superclass of a class with the given class name
     * is within analysis bounds (is user-defined)
     *
     * @param superClass superclass given
     * @return true if class is within analysis bounds,
     * false otherwise
     */
    private boolean withinAnalysisBounds(ResolvedReferenceType superClass) {
        return withinAnalysisBounds(superClass.getQualifiedName());
    }

    /**
     * Calculates all values of metrics the tool supports
     *
     * @param javaClass the class we are referring to
     */
    public void calculateMetrics(ClassOrInterfaceDeclaration javaClass) {
        String superClassName;
        try {
            superClassName = javaClass.getExtendedTypes().get(0).resolve().getQualifiedName();
        } catch (Exception e) {
            superClassName = null;
        }

        try {
            this.classMetrics.setDit(calculateDit(javaClass.resolve().getQualifiedName(), superClassName != null
                    && javaClass.resolve().getAncestors().size() != 0
                    ? javaClass.resolve().getAncestors().get(javaClass.resolve().getAncestors().size() - 1) : null));
        } catch (Exception ignored) {
        }

        this.classMetrics.setDac(calculateDac(javaClass));
        this.classMetrics.setSize2(calculateSize2(javaClass));
        this.classMetrics.setSize1(calculateSize1(javaClass));
        this.classMetrics.setWmcCc(calculateWmcCc(javaClass));
        this.classMetrics.setCbo(this.efferentCoupledClasses.size());

        this.classMetrics.setRfc(this.responseSet.size() + this.classMetrics.getWmc()); //WMC as CIS angor
        this.classMetrics.setMpc(this.methodsCalled.size());    //angor
        this.classMetrics.setLcom(calculateLCOM());
        this.classMetrics.setClassesNum(calculateClassesNum(javaClass));
    }

    /**
     * Calculates all values of metrics the tool supports
     *
     * @param en the enumeration we are referring to
     */
    public void calculateMetrics(EnumDeclaration en) {
        this.classMetrics.setDit(0);

        this.classMetrics.setDac(calculateDac(en));
        this.classMetrics.setSize2(calculateSize2(en));
        this.classMetrics.setSize1(calculateSize1(en));
        this.classMetrics.setWmcCc(calculateWmcCc(en));
        this.classMetrics.setRfc(this.responseSet.size() + this.classMetrics.getWmc()); //WMC as CIS angor
        this.classMetrics.setMpc(this.methodsCalled.size());    //angor
        this.classMetrics.setLcom(calculateLCOM());
        this.classMetrics.setCbo(this.efferentCoupledClasses.size());
    }
}