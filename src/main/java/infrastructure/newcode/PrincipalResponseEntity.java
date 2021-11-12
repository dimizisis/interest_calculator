package infrastructure.newcode;

import java.util.List;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
public class PrincipalResponseEntity {

	public PrincipalResponseEntity(String sha, Integer commitTime, List<DiffEntry> diffEntries) {
		this.sha = sha;
		this.commitTime = commitTime;
		this.diffEntries = diffEntries;
	}

	private String sha;
	private Integer commitTime;

	private List<DiffEntry> diffEntries = null;

	public String getSha() {
		return sha;
	}

	public void setSha(String sha) {
		this.sha = sha;
	}

	public Integer getCommitTime() {
		return commitTime;
	}

	public void setCommitTime(Integer commitTime) {
		this.commitTime = commitTime;
	}

	public List<DiffEntry> getDiffEntries() {
		return diffEntries;
	}

	public void setDiffEntries(List<DiffEntry> diffEntries) {
		this.diffEntries = diffEntries;
	}

}
