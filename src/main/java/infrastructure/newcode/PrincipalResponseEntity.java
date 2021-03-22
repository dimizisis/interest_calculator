package infrastructure.newcode;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
public class PrincipalResponseEntity {

	@SerializedName("sha")
	@Expose
	private String sha;
	@SerializedName("commitTime")
	@Expose
	private Integer commitTime;
	@SerializedName("diffEntries")
	@Expose
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
