package metricsCalculator.containers;

import metricsCalculator.calculator.MetricsCalculator;
import metricsCalculator.metrics.ClassMetrics;
import metricsCalculator.metrics.PackageMetrics;

import java.util.HashMap;
import java.util.Map;

public class PackageMetricsContainer {
	private final Map<String, PackageMetrics> packageToMetric;
	private final Map<String, Map<String, ClassMetrics>> packageClasses;
	private final Map<String, Map<String, PackageMetrics>> packageSubpackages;

	public PackageMetricsContainer() {
		this.packageToMetric = new HashMap<>();
		this.packageClasses = new HashMap<>();
		this.packageSubpackages = new HashMap<>();
	}

	public PackageMetrics getMetrics(String name) {
		PackageMetrics packageMetrics = this.packageToMetric
				.get(name);
		if (packageMetrics == null) {
			packageMetrics = new PackageMetrics();
			this.packageToMetric.put(name, packageMetrics);
		}
		return packageMetrics;
	}

	public boolean containsPackage(String packageName) {
		return (this.packageSubpackages.containsKey(packageName))
				|| (this.packageToMetric.containsKey(packageName));
	}

	public Map<String, ClassMetrics> getPackageClasses(String packageName) {
		return this.packageClasses.computeIfAbsent(packageName, k -> new HashMap<>());
	}

	public Map<String, PackageMetrics> getPackageSubpackages(
			String packageName) {
		return this.packageSubpackages
				.computeIfAbsent(packageName, k -> new HashMap<>());
	}

	public void addClassToPackage(String packageName, String javaClass,
			ClassMetrics javaMetrics) {
		Map<String, ClassMetrics> packageClasses = getPackageClasses(packageName);
		packageClasses.put(javaClass, javaMetrics);
	}

	public void addPackage(String packageName) {
		if (existsInSubPackages(packageName)) {
			return;
		}
		int dotIndex = packageName.lastIndexOf('.');

		if (dotIndex == -1) {
			getPackageSubpackages(packageName);
			String currentProject = MetricsCalculator.getCurrentProject();
			MetricsCalculator.getProjectMetricsContainer()
					.getPackages(currentProject)
					.put(packageName, getMetrics(packageName));
			return;
		}

		String parentPackage = packageName.substring(0, dotIndex);
		String childPackage = packageName;
		while (!parentPackage.isEmpty()) {
			dotIndex = childPackage.lastIndexOf('.');
			if (dotIndex == -1) {
				getPackageSubpackages(parentPackage);
				String currentProject = MetricsCalculator.getCurrentProject();
				MetricsCalculator.getProjectMetricsContainer()
						.getPackages(currentProject)
						.put(parentPackage, getMetrics(parentPackage));
				break;
			}

			parentPackage = childPackage.substring(0, dotIndex);
			if (!existsInSubPackages(childPackage)) {
				getPackageSubpackages(parentPackage).put(childPackage,
						getMetrics(childPackage));
			}
			childPackage = parentPackage;
		}
	}

	private boolean existsInSubPackages(String packageName) {
		String parent = getParentPackage(packageName);
		return (this.packageSubpackages.containsKey(parent))
				&& ((this.packageSubpackages.get(parent))
				.containsKey(packageName));
	}

	private String getParentPackage(String packageName) {
		int packageSeparatorIndex = packageName.lastIndexOf('.');
		if (packageSeparatorIndex == -1)
			return "";
		return packageName.substring(0, packageSeparatorIndex);
	}
}
