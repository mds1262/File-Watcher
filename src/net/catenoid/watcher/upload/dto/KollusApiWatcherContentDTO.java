package net.catenoid.watcher.upload.dto;

import com.google.gson.annotations.Expose;


public class KollusApiWatcherContentDTO {
	@Expose
	public int error;
	@Expose
	public String message;
	@Expose
	public KollusApiWatcherFile result;
	@Override
	public String toString() {
		return "WatcherFileDTO [error=" + error + ", message=" + message + ", result=" + result + "]";
	}
}
