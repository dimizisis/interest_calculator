package metricsCalculator.output;

import metricsCalculator.calculator.MetricsCalculator;
import metricsCalculator.metrics.ClassIdentity;
import metricsCalculator.metrics.ClassMetrics;
import metricsCalculator.metrics.PackageMetrics;
import metricsCalculator.metrics.ProjectMetrics;

import java.io.*;
import java.util.Map.Entry;
import java.util.Set;

import static java.lang.System.exit;

public class PrintResults implements CkjmOutputHandler {

	private PrintStream p;
	private ByteArrayOutputStream os;

	public PrintResults() {
		try {
			os  = new ByteArrayOutputStream();
			this.p = new PrintStream(os);
			printHeader();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Exiting...");
			exit(-1);
		}
	}

	public void printHeader() {
		this.p.print("File;ClassName;");
		this.p.print("WMC;DIT;NOCC;RFC;LCOM;WMC;");						// Chidamber & Kemerer metrics
		this.p.print("NOM;MPC;DAC;SIZE1;SIZE2;CBO\n");	// Li & Henry metrics
	}
	
   public void handleClass(String fileName, String className, ClassMetrics c) {
		try {
			c.setGMOODLowLevel();
			c.setQMOODHighLevel();
			this.p.println(fileName + ";" + className + ";" + c.getCsvOutput());
		} catch (Exception e) {}
   }

	@Override
	public void handleProject(String projectName, ProjectMetrics projectMetrics) {
		Set<Entry<String, PackageMetrics>> rootPackages = MetricsCalculator.getProjectMetricsContainer()
				.getPackages(projectName).entrySet();
		Entry<String, PackageMetrics> currentPackage;
		for (Entry<String, PackageMetrics> rootPackage : rootPackages) {
			currentPackage = rootPackage;
			handlePackage(currentPackage.getKey());
		}
		this.p.close();
	}
	 
  private void handlePackage(String packageName) {
		Set<Entry<String, PackageMetrics>> subPackages = MetricsCalculator.getPackageMetricsContainer()
				.getPackageSubpackages(packageName).entrySet();
		Entry<String, PackageMetrics> currentPackage;
	  for (Entry<String, PackageMetrics> subPackage : subPackages) {
		  currentPackage = subPackage;
		  handlePackage(currentPackage.getKey());
	  }

		Set<Entry<ClassIdentity, ClassMetrics>> classes = MetricsCalculator.getPackageMetricsContainer()
				.getPackageClasses(packageName).entrySet();
		Entry<ClassIdentity, ClassMetrics> currentClass;
	  for (Entry<ClassIdentity, ClassMetrics> aClass : classes) {
		  currentClass = aClass;
		  handleClass(currentClass.getKey().getFilePath(), currentClass.getKey().getClassName(),
				  currentClass.getValue());
	  }
	}

	public String getOutput(){
		try {
			return os.toString("UTF8");
		} catch (UnsupportedEncodingException ignored) {}
		return null;
	}
}
