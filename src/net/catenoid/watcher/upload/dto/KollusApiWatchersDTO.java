package net.catenoid.watcher.upload.dto;

import com.google.gson.annotations.Expose;

public class KollusApiWatchersDTO {
	@Expose
	public int error;
	
	@Expose
	public String message;
	
	public KollusApiWatcherContentDTO watcherFile;
	
	@Expose
	public KollusApiWatcherContentsDTO result;

	@Override
	public String toString() {
		return "ApiResult_WatcherApiDTO [error=" + error + ", message=" + message + ", watcherFile=" + watcherFile.toString()
				+ ", result=" + result.toString() + "]";
	}  
	
	
}


