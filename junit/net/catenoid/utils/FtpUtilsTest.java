package net.catenoid.utils;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.upload.utils.FtpUploadUtils;

public class FtpUtilsTest {
	
	private ArrayList<FileItemDTO> items = null;
	
	private WatcherFolder[] watchers = null;	
	private Config conf = null;
	
	@Before
	public void initSetup() throws Exception {
		this.conf = Config.getConfig();
		this.watchers = conf.getWatchers();
	}
	
	@Before
	public void generateItems() {
		items = new ArrayList<FileItemDTO>();
		
		FileItemDTO f = new FileItemDTO();
		
//		f.set_physical_path("/Users/kollus/upload/deuksoo/test.mp4");
//		f.set_content_path("/deuksoo/20210712-testest.mp4");
		items.add(f);
	}
	
	@Test
	public void moveToWorkDirTest() throws Throwable {
		for(WatcherFolder info : watchers) {
			
			FtpUploadUtils utils = new FtpUploadUtils();
			
			utils.conf = this.conf;
			utils.info = info;
			
			int moveCnt = utils.moveToWorkDir(items);
			
			assertTrue(moveCnt > 0);
		}

	}
}
