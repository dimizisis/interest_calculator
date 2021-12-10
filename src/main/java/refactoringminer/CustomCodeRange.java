package refactoringminer;

import java.util.Objects;

public class CustomCodeRange {
    private final Integer startLine;
    private final Integer endLine;

    public CustomCodeRange(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (Objects.isNull(o))
            return false;
        if (this.getClass() != o.getClass())
            return false;
        CustomCodeRange otherCodeRange = (CustomCodeRange) o;
        return this.getStartLine().equals(otherCodeRange.getStartLine()) &&
                this.getEndLine().equals(otherCodeRange.getEndLine());
    }

    @Override
    public String toString() {
        return  "StartLine: " + this.getStartLine() + " | EndLine: " + this.getEndLine();
    }

    @Override
    public int hashCode() {
        return Objects.hash(startLine, endLine);
    }
}
