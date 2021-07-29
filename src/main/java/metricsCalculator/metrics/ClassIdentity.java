package metricsCalculator.metrics;

import java.util.Objects;

public class ClassIdentity {
    private String className;
    private String filePath;

    public ClassIdentity(String className, String filePath) {
        this.className = className;
        this.filePath = filePath;
    }

    public String getClassName() {
        return className;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassIdentity that = (ClassIdentity) o;
        return Objects.equals(className, that.className) && Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, filePath);
    }

    @Override
    public String toString() {
        return "ClassIdentity{" +
                "className='" + className + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
