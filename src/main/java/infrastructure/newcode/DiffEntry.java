package infrastructure.newcode;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
public class DiffEntry {

	@SerializedName("oldFilePath")
	@Expose
	private String oldFilePath;
	@SerializedName("newFilePath")
	@Expose
	private String newFilePath;
	@SerializedName("changeType")
	@Expose
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

	public void setNewFilePath(String newFilePath) {
		this.newFilePath = newFilePath;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

}
