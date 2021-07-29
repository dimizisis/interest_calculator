package metricsCalculator.output;

import metricsCalculator.metrics.ClassMetrics;
import metricsCalculator.metrics.ProjectMetrics;

public interface CkjmOutputHandler {
    void handleProject(String paramString, ProjectMetrics paramProjectMetrics);
    void handleClass(String paramString1, String paramString2, ClassMetrics paramClassMetrics);
}