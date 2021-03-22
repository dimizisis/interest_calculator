package main;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.Globals;
import infrastructure.interest.JavaFile;
import infrastructure.newcode.DiffEntry;
import infrastructure.newcode.PrincipalResponseEntity;
import metricsCalculator.calculator.MetricsCalculator;

public class Main {
    public static String projectID;
    public static String projectPath;
    public static double K;

    private static final Set<String> rootFolders = ConcurrentHashMap.newKeySet();
    private static final Set<JavaFile> javaFiles = ConcurrentHashMap.newKeySet();
	
    public static void main(String[] args) {
    		projectID="org.apache.bookkeeper:bookkeeper";//args[0];
    		projectPath="C:\\Users\\Nikos\\Desktop\\projects\\bookkeeper";//args[1];
    		K=0.11229844;//Double.parseDouble(args[2]);
    		
	    	getRootFolders();
			getJavaFiles(projectPath);
			System.out.println("number of files: " + javaFiles.size());


			
			getMetricsCalculatorMetrics();
			getFilesForInterest();

//			for (Commit commit : commits) {
//				for (DiffEntry diffEntry : diffEntries) {
//
//				}
//			}

	}

	private List<DiffEntry> getDiffEntriesAtCommit(String commitId) {
		HttpResponse<JsonNode> httpResponse;
		Unirest.setTimeouts(0, 0);
		try {
			httpResponse = Unirest.get("http://195.251.210.147:8989/api/sdk4ed/internal/longest-path/with-commit-changes?url=https://github.com/apache/commons-io&sha=" + commitId).asJson();
			PrincipalResponseEntity[] responseEntities = new Gson().fromJson(httpResponse.getBody().toString(), PrincipalResponseEntity[].class);
			List<DiffEntry> diffEntries = new ArrayList<>();
			for (PrincipalResponseEntity entity : responseEntities)
				diffEntries.addAll(entity.getDiffEntries());
			return diffEntries;
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void getFilesForInterest() {
		javaFiles
				.stream()
				.filter(jf -> jf.getQualityMetrics().getDIT() != -1)
				.forEach(Globals::addJavaFile);
	}

	/**
     * Get Folders of the root dir of the project
     */
    private static void getRootFolders() {
        // Get all files from a directory.
		Arrays.stream(Objects.requireNonNull(new File(projectPath).listFiles()))
				.filter(File::isDirectory)
				.forEach(file -> rootFolders.add(file.getAbsolutePath().replace(projectPath, "")));
	}

	/**
     * Finds all the files in the directory that will be analyzed
     * @param directoryName the directory to search for files
     */
    private static void getJavaFiles(String directoryName){
        File directory = new File(directoryName);
        // Get all files from a directory.
        Set<File> fList = new HashSet<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));

		for (File file : fList) {
			if (file.isFile() && file.getName().contains(".") && file.getName().charAt(0)!='.') {
				String[] str=file.getName().split("\\.");
				// For all the files of this directory get the extension
				if(str[str.length-1].equalsIgnoreCase("java") )
					javaFiles.add(new JavaFile(file.getAbsolutePath().replace(projectPath, "")) );
			} else if (file.isDirectory())
				getJavaFiles(file.getAbsolutePath());
		}
	}
    
    /**
     * Get Metrics from Metrics Calculator for every java file
     */
    private static void getMetricsCalculatorMetrics() {
    	MetricsCalculator.start(projectPath);
		
		String[] s = MetricsCalculator.printResults().split("\\r?\\n");
		for(int i=1; i<s.length; ++i) {
			String[] column = s[i].split(";");
			
			String filePath = column[0].replace(".", "/")+".java";
			
			for(JavaFile jf: javaFiles) {
				if(jf.getPath().endsWith(filePath)) {
					jf.getQualityMetrics().setDIT(Integer.parseInt(column[1]));
					jf.getQualityMetrics().setNOCC(Integer.parseInt(column[2]));
					jf.getQualityMetrics().setRFC(Double.parseDouble(column[3]));
					jf.getQualityMetrics().setLCOM(Double.parseDouble(column[4]));
					jf.getQualityMetrics().setWMC(Double.parseDouble(column[5]));
					jf.getQualityMetrics().setNOM(Double.parseDouble(column[6]));
					jf.getQualityMetrics().setMPC(Integer.parseInt(column[7]));
					jf.getQualityMetrics().setDAC(Integer.parseInt(column[8]));
					jf.getQualityMetrics().setSIZE1(Integer.parseInt(column[9]));
					jf.getQualityMetrics().setSIZE2(Integer.parseInt(column[10]));
					jf.getQualityMetrics().setClassesNum(Integer.parseInt(column[11]));
					break;
				}
			}
		}
	}
}
