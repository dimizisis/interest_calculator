package main;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
	private static final String GIT_SERVICE_URL = "https://github.com/";
	private static final String OWNER = "apache";
	private static final String REPOSITORY = "commons-io";
	private static final String CLONE_PATH = "C:/Users/Dimitris/Desktop/";
	private static final String PROJECT_PATH = CLONE_PATH + REPOSITORY;
	
    public static void main(String[] args) {

		PrincipalResponseEntity[] responseEntities = getResponseEntities();
//		checkout (clonePath, responseEntities[0].getSha());
		Set<JavaFile> javaFiles = getJavaFiles(PROJECT_PATH);
		setMetrics(PROJECT_PATH, javaFiles);
		Globals.setJavaFiles(getFilesForInterest(javaFiles));

		for (int i = 1; i < Objects.requireNonNull(responseEntities).length; ++i) {
//			checkout (clonePath, responseEntities[i].getSha());
			removeDeletedFiles(responseEntities[i].getDiffEntries());
			Set<JavaFile> newFiles = findNewFiles(responseEntities[i].getDiffEntries());
			Set<JavaFile> modifiedFiles = findModifiedFiles(responseEntities[i].getDiffEntries());
			setMetrics(PROJECT_PATH, newFiles);
			setMetrics(PROJECT_PATH, modifiedFiles);
			newFiles = getFilesForInterest(newFiles);
			modifiedFiles = getFilesForInterest(modifiedFiles);
			Globals.getJavaFiles().addAll(newFiles);
			Globals.getJavaFiles().addAll(modifiedFiles);
		}

	}

	private static void removeDeletedFiles(List<DiffEntry> diffEntries) {
    	diffEntries
				.stream()
				.filter(diffEntry -> diffEntry.getChangeType().equals("DELETE"))
				.forEach(diffEntry -> Globals.getJavaFiles().removeIf(javaFile -> javaFile.getPath().equals(diffEntry.getOldFilePath())));
	}

	private static Set<JavaFile> findNewFiles(List<DiffEntry> diffEntries) {
    	Set<JavaFile> newFiles = ConcurrentHashMap.newKeySet();
		diffEntries
				.stream()
				.filter(diffEntry -> diffEntry.getChangeType().equals("ADD"))
				.forEach(diffEntry -> newFiles.add(new JavaFile(diffEntry.getNewFilePath())));
		return newFiles;
	}

	private static Set<JavaFile> findModifiedFiles(List<DiffEntry> diffEntries) {
		Set<JavaFile> modifiedFiles = ConcurrentHashMap.newKeySet();
		diffEntries
				.stream()
				.filter(diffEntry -> diffEntry.getChangeType().equals("MODIFY"))
				.forEach(diffEntry -> {
					Globals.getJavaFiles().removeIf(javaFile -> javaFile.getPath().equals(diffEntry.getOldFilePath()));
					modifiedFiles.add(new JavaFile(diffEntry.getNewFilePath()));
				});
		return modifiedFiles;
	}

//	private static Repository cloneRepository() throws Exception {
//		Git git = Git.cloneRepository()
//				.setURI( "https://github.com/eclipse/jgit.git" )
//				.setDirectory( new File("/path/to/repo") )
//				.call();
//	}

	private static PrincipalResponseEntity[] getResponseEntities() {
		HttpResponse<JsonNode> httpResponse;
		Unirest.setTimeouts(0, 0);
		try {
			httpResponse = Unirest.get("http://195.251.210.147:8989/api/sdk4ed/internal/longest-path/with-commit-changes?url=" + GIT_SERVICE_URL + OWNER).asJson();
			return new Gson().fromJson(httpResponse.getBody().toString(), PrincipalResponseEntity[].class);
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Set<JavaFile> getFilesForInterest(Set<JavaFile> javaFiles) {
		return javaFiles
				.stream()
				.filter(jf -> jf.getQualityMetrics().getDIT() != -1)
				.collect(Collectors.toSet());
	}

	/**
     * Finds all the files in the directory that will be analyzed
     * @param directoryName the directory to search for files
     */
    private static Set<JavaFile> getJavaFiles(String directoryName){
        File directory = new File(directoryName);
        /* Get all files from a directory */
        Set<File> fList = new HashSet<>(Arrays.asList(Objects.requireNonNull(directory.listFiles())));
        Set<JavaFile> jfs = ConcurrentHashMap.newKeySet();

		for (File file : fList) {
			if (file.isFile() && file.getName().contains(".") && file.getName().charAt(0)!='.') {
				String[] str = file.getName().split("\\.");
				/* For all the files of this directory get the extension */
				if(str[str.length-1].equalsIgnoreCase("java") )
					jfs.add(new JavaFile(file.getAbsolutePath().replace(directoryName, "")) );
			} else if (file.isDirectory())
				getJavaFiles(file.getAbsolutePath());
		}
		return jfs;
	}
    
    /**
     * Get Metrics from Metrics Calculator for every java file
     */
    private static void setMetrics(String projectPath, Set<JavaFile> jfs) {
		for (JavaFile jf : jfs) {
			MetricsCalculator.start(projectPath, jf.getPath());
			String[] s = MetricsCalculator.printResults().split("\\r?\\n");
			for(int i=1; i < s.length; ++i) {
				String[] column = s[i].split(";");
				String filePath = column[0].replace(".", "/")+".java";
				registerMetrics(column, filePath);
			}
		}
	}

	private static void registerMetrics(String[] column, String filePath) {
		for (JavaFile jf : Globals.getJavaFiles()) {
			if (jf.getPath().endsWith(filePath)) {
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
				jf.calculateInterest();
				break;
			}
		}
	}
}
