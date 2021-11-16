package net.catenoid.watcher.upload.dto;

import java.util.Arrays;

import com.google.gson.annotations.Expose;


public class KollusApiWatcherContentsDTO {
	@Expose
	public int error_code;
	
	@Expose
	public String error_detail;
	
	@Expose
	public KollusApiWatcherContentDTO[] watcher_files;

	@Override
	public String toString() {
		return "HttpResults [error_code=" + error_code + ", error_detail=" + error_detail + ", watcher_files="
				+ Arrays.toString(watcher_files).toString() + "]";
	}
	
	
}
