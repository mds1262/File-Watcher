package net.catenoid.watcher.upload.dto;

public class ExtFilenameResultDTO {
	
	private String category;
	private String filename;
	private String fullpath;
	private String uploadPath;
	private String title;
	private String contentProviderKey;
	private String extension;
	private String mediaProfileKey;
	private boolean audioPath;
	private boolean encryptPath;
	private boolean passthroughPath;
	private boolean uploadFileKeyPath;
	private boolean mediaContentKeyPath;
	private boolean error;
	
	public ExtFilenameResultDTO() {};

	public ExtFilenameResultDTO(ExtFilenameParserDTO p) {
		this.category = p.category();
		this.filename = p.filename();
		this.fullpath = p.fullpath();
		this.uploadPath = p.upload_path();
		this.title = p.title();
		this.contentProviderKey = p.content_provider_key();
		this.extension = p.extension();
		this.mediaProfileKey = p.media_profile_key();
		this.audioPath = p.isAudioPath();
		this.encryptPath = p.isEncryptPath();
		this.passthroughPath = p.isPassthrough();
		this.uploadFileKeyPath = p.isUploadFileKeyFolder();
		this.mediaContentKeyPath = p.isMediaContentKeyFolder();
		this.error = p.isError();
	}


	public String getCategory() {
		return category;
	}


	public String getFilename() {
		return filename;
	}


	public String getFullpath() {
		return fullpath;
	}


	public String getUploadPath() {
		return uploadPath;
	}


	public String getTitle() {
		return title;
	}


	public String getContentProviderKey() {
		return contentProviderKey;
	}


	public String getExtension() {
		return extension;
	}


	public String getMediaProfileKey() {
		return mediaProfileKey;
	}


	public boolean isAudioPath() {
		return audioPath;
	}


	public boolean isEncryptPath() {
		return encryptPath;
	}


	public boolean isPassthroughPath() {
		return passthroughPath;
	}


	public boolean isUploadFileKeyPath() {
		return uploadFileKeyPath;
	}


	public boolean isMediaContentKeyPath() {
		return mediaContentKeyPath;
	}


	public boolean isError() {
		return error;
	}	
	
}
