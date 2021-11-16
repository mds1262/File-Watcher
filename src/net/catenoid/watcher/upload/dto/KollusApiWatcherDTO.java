package net.catenoid.watcher.upload.dto;

import com.google.gson.annotations.Expose;

public class KollusApiWatcherDTO {
	@Expose
	public int error;
	
	@Expose
	public String message;
	
	@Expose
	public FileItemDTO result;
}
