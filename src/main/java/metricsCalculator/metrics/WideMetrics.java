package metricsCalculator.metrics;

public class WideMetrics extends Metrics {
	protected int classesCount;
	protected int damValidClassesCount;
	protected boolean calculated;
	protected float dsc;
	protected int noh;

	public WideMetrics() {
		this.dsc = 0.0F;
		this.noh = 0;
		this.classesCount = 0;
		this.damValidClassesCount = 0;
		this.calculated = false;
	}

	public int getClassesCount() {
		return this.classesCount;
	}

	public void setClassesCount(int classesCount) {
		this.classesCount = classesCount;
	}

	public void setDsc(float dsc) {
		this.dsc = dsc;
	}

	public int getNoh() {
		return this.noh;
	}

	public void setNoh(int noh) {
		this.noh = noh;
	}

	public boolean isCalculated() {
		return this.calculated;
	}

	public void setCalculated(boolean calculated) { this.calculated = calculated; }

	public int getDamValidClassesCount() { return damValidClassesCount; }

	public float getDsc() { return dsc; }

	public void setDamValidClassesCount(int damValidClassesCount) { this.damValidClassesCount = damValidClassesCount; }
}
