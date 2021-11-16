package net.catenoid.watcher.upload;


public interface KusUploadService extends CommonUploadService{
	public void moveToWorkFiles() throws Exception;
	public int	sendCompleteWorkFiles() throws Exception;
}
