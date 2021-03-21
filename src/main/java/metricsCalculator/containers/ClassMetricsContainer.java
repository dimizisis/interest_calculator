package metricsCalculator.containers;

import metricsCalculator.metrics.ClassMetrics;

import java.util.HashMap;
import java.util.Map;

public class ClassMetricsContainer {
	private final Map<String, ClassMetrics> classToMetrics = new HashMap<>();

	public ClassMetrics getMetrics(String name) {
		ClassMetrics cm = this.classToMetrics.get(name);
		if (cm == null) {
			cm = new ClassMetrics();
			this.classToMetrics.put(name, cm);
		}
		return cm;
	}
}
