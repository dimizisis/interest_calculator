package metricsCalculator.containers;

import metricsCalculator.metrics.PackageMetrics;
import metricsCalculator.metrics.ProjectMetrics;

import java.util.HashMap;
import java.util.Map;

public class ProjectMetricsContainer {
	private final Map<String, ProjectMetrics> projectToMetric;
	private final Map<String, Map<String, PackageMetrics>> projectPackages;

	public ProjectMetricsContainer() {
		this.projectToMetric = new HashMap<>();
		this.projectPackages = new HashMap<>();
	}

	public Map<String, ProjectMetrics> getProjects() {
		return this.projectToMetric;
	}

	public ProjectMetrics getMetrics(String name) {
		ProjectMetrics pm = this.projectToMetric.get(name);
		if (pm == null) {
			pm = new ProjectMetrics();
			this.projectToMetric.put(name, pm);
		}
		return pm;
	}

	public Map<String, PackageMetrics> getPackages(String name) {
		return this.projectPackages.computeIfAbsent(name, k -> new HashMap<>());
	}
}
