package metricsCalculator.metrics;

import metricsCalculator.calculator.MetricsCalculator;

import java.util.Map.Entry;
import java.util.Set;

public class PackageMetrics extends WideMetrics {
	public void calculateAllMetrics(String packageName) {
		if (isCalculated())
			return;
		this.setClassesCount(this.getClassesCount() + MetricsCalculator.getPackageMetricsContainer()
				.getPackageClasses(packageName).size());

		Set<Entry<String, PackageMetrics>> subPackages = MetricsCalculator.getPackageMetricsContainer()
				.getPackageSubpackages(packageName).entrySet();
		Entry<String, PackageMetrics> currentPackage;

		for (Entry<String, PackageMetrics> subPackage : subPackages) {
			currentPackage = subPackage;
			currentPackage.getValue()
					.calculateAllMetrics(currentPackage.getKey());
			this.setClassesCount(this.getClassesCount() + currentPackage.getValue().getClassesCount());
			increaseMetrics(currentPackage.getValue());
		}

		Set<Entry<ClassIdentity, ClassMetrics>> classes = MetricsCalculator.getPackageMetricsContainer()
				.getPackageClasses(packageName).entrySet();
		Entry<ClassIdentity, ClassMetrics> currentClass;
		for (Entry<ClassIdentity, ClassMetrics> aClass : classes) {
			currentClass = aClass;
			increaseMetrics(currentClass.getValue());
		}
		finalizeMetrics();
		this.setCalculated(true);
	}

	private void increaseMetrics(ClassMetrics classMetrics) {
		try {
			this.setAna(this.getAna() + classMetrics.getAna());
			this.setCbo(this.getCbo() + classMetrics.getCbo());
			this.setMoa(this.getMoa() + classMetrics.getMoa());
			this.setNop(this.getNop() + classMetrics.getNop());
			this.setLcom(this.getLcom() + classMetrics.getLcom());
			this.setNpm(this.getNpm() + classMetrics.getNpm());
			this.setWmc(this.getWmc() + classMetrics.getWmc());
			this.setMfa(this.getMfa() + classMetrics.getMfa());
			if (!Float.isNaN(classMetrics.getDam())) {
				if (Float.isNaN(getDam()))
					this.setDam(0.0F);
				this.setDam(this.getDam() + classMetrics.getDam());
				this.setClassesCount(this.getClassesCount() + 1);
			}
			if ((classMetrics.getAna() == 0.0F) && (classMetrics.getNoc() > 0))
				this.setNoh((int) ((float) (this.getNoh() + 1.0D)));
			this.setDsc(this.getDsc() + 1.0F);
		} catch (NullPointerException e) {}
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
			this.setDam(this.getDam() + packageMetrics.getDam()
					* packageMetrics.getDamValidClassesCount());
			this.setDamValidClassesCount(this.getDamValidClassesCount() + packageMetrics.getDamValidClassesCount());
		}

		this.setNoh(this.getNoh() + packageMetrics.getNoh());
		this.setDsc(this.getDsc() + packageMetrics.getDsc());
	}

	private void finalizeMetrics() {
		this.setAna(this.getAna() / this.getClassesCount());
		this.setCbo(this.getCbo() / this.getClassesCount());
		this.setMoa(this.getMoa() / this.getClassesCount());
		this.setNop(this.getNop() / this.getClassesCount());
		this.setLcom(this.getLcom() / this.getClassesCount());
		this.setNpm( this.getNpm() / this.getClassesCount());
		this.setWmc(this.getWmc() / this.getClassesCount());
		this.setMfa(this.getMfa() / this.getClassesCount());
		this.setDam(this.getDam() / this.getDamValidClassesCount());
	}
}