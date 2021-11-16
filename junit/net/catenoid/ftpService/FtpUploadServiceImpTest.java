package net.catenoid.ftpService;

import org.junit.Before;
import org.junit.Test;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.upload.FtpUploadService;
import net.catenoid.watcher.uploadImp.FtpUploadServiceImp;

public class FtpUploadServiceImpTest {
	
	private WatcherFolder[] watchers = null;	
	private Config conf = null;
	
	
	@Before
	public void initSetup() {
		this.conf = Config.getConfig();
		this.watchers = conf.getWatchers();
	}
	
	@Test
	public void renewWorkFileListTest() throws Throwable {
		for(WatcherFolder info : watchers) {
			FtpUploadService service = new FtpUploadServiceImp(info, conf);
			service.renewWorkFileList();
			
		}
	}
	
	@Test
	public void findWorkFileListTest() throws Throwable {
		for(WatcherFolder info : watchers) {
			FtpUploadService service = new FtpUploadServiceImp(info, conf);
			service.findWorkFileList();
			
		}
	}

}
