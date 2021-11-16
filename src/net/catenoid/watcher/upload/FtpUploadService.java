package net.catenoid.watcher.upload;

public interface FtpUploadService extends CommonUploadService {
	
	void renewWorkFileList() throws Exception;
	
}
