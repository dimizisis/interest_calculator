package infrastructure.newcode;

import java.util.Objects;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */

public class DiffEntry {

	public DiffEntry(String oldFilePath, String newFilePath, String changeType) {
		this.oldFilePath = oldFilePath;
		this.newFilePath = newFilePath;
		this.changeType = changeType;
	}

	private String oldFilePath;
	private String newFilePath;
	private String changeType;

	public String getOldFilePath() {
		return oldFilePath;
	}

	public void setOldFilePath(String oldFilePath) {
		this.oldFilePath = oldFilePath;
	}

	public String getNewFilePath() {
		return newFilePath;
	}

	public void setNewFilePathString (String newFilePath) {
		this.newFilePath = newFilePath;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DiffEntry diffEntry = (DiffEntry) o;
		return Objects.equals(oldFilePath, diffEntry.oldFilePath) && Objects.equals(newFilePath, diffEntry.newFilePath) && Objects.equals(changeType, diffEntry.changeType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(oldFilePath, newFilePath, changeType);
	}
}