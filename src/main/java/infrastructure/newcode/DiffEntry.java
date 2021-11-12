package infrastructure.newcode;

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

}