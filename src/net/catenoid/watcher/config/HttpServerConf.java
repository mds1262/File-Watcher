package net.catenoid.watcher.config;

import java.util.ArrayList;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.kollus.json_data.HttpdSection;

public class HttpServerConf extends HttpdSection {

	@Expose
	@SerializedName("enabled")
	private boolean enabled;
	
	@Expose
	@SerializedName("ftp.dir")
	private ArrayList<String> ftpDir;
	
	private ArrayList<String> userDir;
	
	@Expose
	@SerializedName("snapshot.dir")
	private String snapshotDir;
	
	@Expose
	@SerializedName("thumbnail.dir")
	private String thumbnailDir;
	
	@Expose
	@SerializedName("working.dir")
	private String workingDir;
	
	@Expose
	@SerializedName("backup.dir")
	private ArrayList<String> backupDir;

	@Expose
	@SerializedName("content.dir")
	private ArrayList<String> contentDir;
		
	private HttpServerConf() {
		
	}
	
	public void _init() {
		setUserDir();
		init_allowip();
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public ArrayList<String> getFtpDir() {
		return ftpDir;
	}

	public ArrayList<String> getContentDir() {
		return contentDir;
	}

	public ArrayList<String> getUserDir() {
		return userDir;
	}

	public void setUserDir() {
		userDir = new ArrayList<String>(getBackupDir());
		userDir.add(getSnapshotDir());
		userDir.add(getThumbnailDir());
		userDir.add(getWorkingDir());
	}

	public ArrayList<String> getBackupDir() {
		return backupDir;
	}

	public String getWorkingDir() {
		return workingDir;
	}

	public String getThumbnailDir() {
		return thumbnailDir;
	}

	public String getSnapshotDir() {
		return snapshotDir;
	}

}
