package infrastructure;

import infrastructure.interest.JavaFile;

import java.io.File;
import java.util.*;

/**
 * @author Dimitrios Zisis <zisisndimitris@gmail.com>
 */
public class Project {

    private String url;
    private String owner;
    private String repo;
    private String clonePath;

    public Project(String url, String clonePath) {
        this.url = url;
        this.owner = getRepositoryOwner();
        this.repo = getRepositoryName();
        this.clonePath = clonePath;
    }

    public Project(String url) {
        this.url = url;
        this.owner = getRepositoryOwner();
        this.repo = getRepositoryName();
//        this.clonePath = File.pathSeparator + "tmp" + File.pathSeparator + getRepositoryOwner() + File.pathSeparator + getRepositoryName();
        this.clonePath = "C:/Users/Dimitris/Desktop/test";
    }

    public Project(String url, String owner, String repo, String clonePath) {
        this.url = url;
        this.owner = owner;
        this.repo = repo;
        this.clonePath = clonePath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getClonePath() {
        return clonePath;
    }

    public void setClonePath(String clonePath) {
        this.clonePath = clonePath;
    }

    private String getRepositoryOwner() {
        String newURL = preprocessURL();
        String[] urlSplit = newURL.split("/");
        return urlSplit[urlSplit.length - 2].replaceAll(".*@.*:", "");
    }

    private String getRepositoryName() {
        String newURL = preprocessURL();
        String[] urlSplit = newURL.split("/");
        return urlSplit[urlSplit.length - 1];
    }

    private String preprocessURL() {
        String newURL = this.getUrl();
        if (newURL.endsWith(".git/"))
            newURL = newURL.replace(".git/", "");
        if (newURL.endsWith(".git"))
            newURL = newURL.replace(".git", "");
        if (newURL.endsWith("/"))
            newURL = newURL.substring(0, newURL.length() - 1);
        return newURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(url, project.url) && Objects.equals(owner, project.owner) && Objects.equals(repo, project.repo) && Objects.equals(clonePath, project.clonePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, owner, repo, clonePath);
    }
}
