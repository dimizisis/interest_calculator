package metricsCalculator.metrics;

import metricsCalculator.calculator.MetricsCalculator;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ProjectMetrics extends WideMetrics {
	public void calculateAllMetrics(String project) {
		if (isCalculated()) {
			return;
		}
		Map<String, PackageMetrics> packageMetrics = MetricsCalculator.getProjectMetricsContainer()
				.getPackages(project);
		Set<Entry<String, PackageMetrics>> rootPackages = packageMetrics.entrySet();
		Entry<String, PackageMetrics> currentPackage;
		for (Entry<String, PackageMetrics> rootPackage : rootPackages) {
			currentPackage = rootPackage;
			(currentPackage.getValue())
					.calculateAllMetrics(currentPackage.getKey());
			this.setClassesCount(this.getClassesCount() + currentPackage.getValue().getClassesCount());
			increaseMetrics(currentPackage.getValue());
		}
		finalizeMetrics();
		this.setCalculated(true);
	}

	private void finalizeMetrics() {
		this.setAna(this.getAna() / this.getClassesCount());
		this.setCbo(this.getCbo() / this.getClassesCount());
		this.setMoa(this.getMoa() / this.getClassesCount());
		this.setNop(this.getNop() / this.getClassesCount());
		this.setLcom(this.getLcom() / this.getClassesCount());
		this.setNpm(this.getNpm() / this.getClassesCount());
		this.setWmc(this.getWmc() / this.getClassesCount());
		this.setMfa(this.getMfa() / this.getClassesCount());
		this.setDam(this.getDam() / this.getDamValidClassesCount());
	}

	private void increaseMetrics(PackageMetrics packageMetrics) {
		this.setAna(this.getAna() + packageMetrics.getAna() * packageMetrics.getClassesCount());
		this.setCbo(this.getCbo() + packageMetrics.getCbo() * packageMetrics.getClassesCount());
		this.setMoa(this.getMoa() + packageMetrics.getMoa() * packageMetrics.getClassesCount());
		this.setNop(this.getNop() + packageMetrics.getNop() * packageMetrics.getClassesCount());
		this.setLcom(this.getLcom() + packageMetrics.getLcom() * packageMetrics.getClassesCount());
		this.setNpm(this.getNpm() + packageMetrics.getNpm() * packageMetrics.getClassesCount());
		this.setWmc(this.getWmc() + packageMetrics.getWmc() * packageMetrics.getClassesCount());
		this.setMfa(this.getMfa() + packageMetrics.getMfa() * packageMetrics.getClassesCount());
		if (!Float.isNaN(packageMetrics.getDam())) {
			if (Float.isNaN(this.getDam()))
				this.setDam(0.0F);
			this.setDam(this.getDam() + packageMetrics.getDam() * packageMetrics.getDamValidClassesCount());
			this.setDamValidClassesCount(this.getDamValidClassesCount() + packageMetrics.getDamValidClassesCount());
		}
		this.setNoh(this.getNoh() + packageMetrics.getNoh());
		this.setDsc(this.getDsc() + packageMetrics.getDsc());
	}
}
