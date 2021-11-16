package net.catenoid.watcher.upload.dto;

import com.google.gson.annotations.Expose;

import net.catenoid.watcher.upload.utils.Poster;

public class KollusApiWatcherFile {
	@Expose
	public int error_code;
	
	@Expose
	public String error_detail;
	
	@Expose
	public String content_provider_key;
	
	@Expose
	public String key;
	
	@Expose
	public int media_content_id;
	
	@Expose
	public String content_path;
	
	@Expose
	public String upload_path;
	
	@Expose
	public int is_audio_file;

    @Expose
    public int checksum_type;
	
	@Expose
	public String snapshot_path;
	
	@Expose
	public String physical_path;
	
	@Expose
	public String deleted_watcher_file_upload_url;
	
	@Expose
	public Poster poster;
	
	@Override
	public String toString() {
		return "HttpResultDTO [error_code=" + error_code + ", error_detail=" + error_detail + ", content_provider_key="
				+ content_provider_key + ", key=" + key + ", media_content_id=" + media_content_id + ", content_path="
				+ content_path + ", upload_path=" + upload_path + ", is_audio_file=" + is_audio_file
				+ ", checksum_type=" + checksum_type + ", snapshot_path=" + snapshot_path + ", physical_path="
				+ physical_path + ", deleted_watcher_file_upload_url=" + deleted_watcher_file_upload_url + ", poster="
				+ poster + "]";
	}
}
