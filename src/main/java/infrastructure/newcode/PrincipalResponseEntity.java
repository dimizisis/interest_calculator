package infrastructure.newcode;

import java.util.Objects;
import java.util.Set;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
public class PrincipalResponseEntity {

	public PrincipalResponseEntity(String sha, Integer commitTime, Set<DiffEntry> addDiffEntries, Set<DiffEntry> modifyDiffEntries, Set<DiffEntry> deleteDiffEntries) {
		this.sha = sha;
		this.commitTime = commitTime;
		this.addDiffEntries = addDiffEntries;
		this.modifyDiffEntries = modifyDiffEntries;
		this.deleteDiffEntries = deleteDiffEntries;
	}

	private String sha;
	private Integer commitTime;

	private Set<DiffEntry> addDiffEntries = null;
	private Set<DiffEntry> modifyDiffEntries = null;
	private Set<DiffEntry> deleteDiffEntries = null;

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

	public Set<DiffEntry> getAddDiffEntries() {
		return addDiffEntries;
	}

	public void setAddDiffEntries(Set<DiffEntry> addDiffEntries) {
		this.addDiffEntries = addDiffEntries;
	}

	public Set<DiffEntry> getModifyDiffEntries() {
		return modifyDiffEntries;
	}

	public void setModifyDiffEntries(Set<DiffEntry> modifyDiffEntries) {
		this.modifyDiffEntries = modifyDiffEntries;
	}

	public Set<DiffEntry> getDeleteDiffEntries() {
		return deleteDiffEntries;
	}

	public void setDeleteDiffEntries(Set<DiffEntry> deleteDiffEntries) {
		this.deleteDiffEntries = deleteDiffEntries;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PrincipalResponseEntity that = (PrincipalResponseEntity) o;
		return Objects.equals(sha, that.sha) && Objects.equals(commitTime, that.commitTime) && Objects.equals(addDiffEntries, that.addDiffEntries) && Objects.equals(modifyDiffEntries, that.modifyDiffEntries) && Objects.equals(deleteDiffEntries, that.deleteDiffEntries);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sha, commitTime, addDiffEntries, modifyDiffEntries, deleteDiffEntries);
	}
}
