package main;

public class JavaFile {
	private String path;
	private QualityMetrics qualityMetrics;
	
	public JavaFile(String path) {
		this.path = path;
		qualityMetrics = new QualityMetrics();
	}

	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }

	public QualityMetrics getQualityMetrics() { return qualityMetrics; }
	public void setQualityMetrics(QualityMetrics qualityMetrics) { this.qualityMetrics = qualityMetrics; }
}
