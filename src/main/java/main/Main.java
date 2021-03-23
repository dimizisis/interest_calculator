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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

public class Main {

	private static final String GIT_SERVICE_URL = "https://github.com/";
	private static final String OWNER = "dimizisis";
	private static final String REPOSITORY = "jcommander3";
	private static final String CLONE_PATH = "C:/Users/Dimitris/Desktop/" + REPOSITORY;
	
    public static void main(String[] args) throws Exception {

    	Git git = cloneRepository(GIT_SERVICE_URL + OWNER + "/" + REPOSITORY, CLONE_PATH);
		PrincipalResponseEntity[] responseEntities = getResponseEntities();
		checkout (git, Objects.requireNonNull(responseEntities)[0].getSha());
		setMetrics(CLONE_PATH);

		for (int i = 1; i < Objects.requireNonNull(responseEntities).length; ++i) {
			checkout (git, responseEntities[i].getSha());
			removeDeletedFiles(responseEntities[i].getDiffEntries());
			Set<JavaFile> newFiles = findNewFiles(CLONE_PATH, responseEntities[i].getDiffEntries());
			Set<JavaFile> modifiedFiles = findModifiedFiles(responseEntities[i].getDiffEntries());
			setMetrics(CLONE_PATH, newFiles);
			setMetrics(CLONE_PATH, modifiedFiles);
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
				.forEach(diffEntry -> Globals.getJavaFiles().removeIf(javaFile -> javaFile.getPath().endsWith(diffEntry.getOldFilePath())));
	}

	private static Set<JavaFile> findNewFiles(String projectPath, List<DiffEntry> diffEntries) {
    	Set<JavaFile> newFiles = ConcurrentHashMap.newKeySet();
		diffEntries
				.stream()
				.filter(diffEntry -> diffEntry.getChangeType().equals("ADD"))
				.forEach(diffEntry -> newFiles.add(new JavaFile(projectPath.replace("\\", "/") + "/" + diffEntry.getNewFilePath())));
		return newFiles;
	}

	private static Set<JavaFile> findModifiedFiles(List<DiffEntry> diffEntries) {
		Set<JavaFile> modifiedFiles = ConcurrentHashMap.newKeySet();
		diffEntries
				.stream()
				.filter(diffEntry -> diffEntry.getChangeType().equals("MODIFY"))
				.forEach(diffEntry -> {
					for (JavaFile javaFile : Globals.getJavaFiles()) {
						if (javaFile.getPath().endsWith(diffEntry.getOldFilePath())) {
							javaFile.setPath(javaFile.getPath().replace(diffEntry.getOldFilePath(), diffEntry.getNewFilePath()));
							modifiedFiles.add(javaFile);
						}
					}
					Globals.getJavaFiles().removeIf(javaFile -> javaFile.getPath().endsWith(diffEntry.getOldFilePath()));
				});
		return modifiedFiles;
	}

	private static Git cloneRepository(String gitUrl, String clonePath) throws Exception {
    	try {
			return Git.cloneRepository()
					.setURI(gitUrl)
					.setDirectory(new File(clonePath))
					.call();
		} catch (Exception e) { return Git.open(new File(clonePath)); }
	}

	private static void checkout(Git git, String commitId) throws GitAPIException {
		git.checkout().setCreateBranch(true).setName(commitId).call();
	}

	private static PrincipalResponseEntity[] getResponseEntities() {
		HttpResponse<JsonNode> httpResponse;
		Unirest.setTimeouts(0, 0);
		try {
			httpResponse = Unirest.get("http://195.251.210.147:8989/api/sdk4ed/internal/longest-path/with-commit-changes?url=" + GIT_SERVICE_URL + OWNER + "/" + REPOSITORY).asJson();
			return new Gson().fromJson(httpResponse.getBody().toString(), PrincipalResponseEntity[].class);
		} catch (UnirestException e) { e.printStackTrace(); }
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
	private static void getJavaFiles(String directoryName, Set<JavaFile> javaFiles) {
		File directory = new File(directoryName);

		// Get all files from a directory.
		File[] fList = directory.listFiles();
		if (fList != null)
			for (File file : fList) {
				if (file.isFile() && file.getAbsolutePath().endsWith(".java")) {
					javaFiles.add(new JavaFile(file.getAbsolutePath().replace("\\", "/")));
				} else if (file.isDirectory()) {
					getJavaFiles(file.getAbsolutePath(), javaFiles);
				}
			}
	}
    
    /**
     * Get Metrics from Metrics Calculator for every java file
     */
    private static void setMetrics(String projectPath, Set<JavaFile> jfs) {
		for (JavaFile jf : jfs) {
			MetricsCalculator.start(projectPath, jf.getPath().replace("\\", "/"));
			String st = MetricsCalculator.printResults();
			String[] s = st.split("\\r?\\n");
			for(int i=1; i < s.length; ++i) {
				String[] column = s[i].split(";");
				String filePath = column[0].replace(".", "/") + ".java";
				registerMetrics(column, filePath, jfs);
			}
		}
	}

	/**
     * Get Metrics from Metrics Calculator for every java file
     */
    private static void setMetrics(String projectPath) {
		MetricsCalculator.start(projectPath);
		String st = MetricsCalculator.printResults();
		String[] s = st.split("\\r?\\n");
		for (int i = 1; i < s.length; ++i) {
			String[] column = s[i].split(";");
			String filePath = column[0].replace(".", "/") + ".java";
			registerMetrics(column, filePath);
		}
	}

	private static void registerMetrics(String[] column, String filePath) {
    	JavaFile jf = new JavaFile(filePath);
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
		Globals.getJavaFiles().add(jf);
	}

	private static void registerMetrics(String[] column, String filePath, Set<JavaFile> jfs) {
		for (JavaFile jf : jfs) {
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
				if (!Globals.getJavaFiles().isEmpty())
					jf.calculateInterest();
				break;
			}
		}
	}
}
