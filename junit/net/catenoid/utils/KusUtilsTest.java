package net.catenoid.utils;

import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.upload.FtpUploadService;
import net.catenoid.watcher.upload.KusUploadService;
import net.catenoid.watcher.upload.utils.KusUploadUtils;
import net.catenoid.watcher.uploadImp.FtpUploadServiceImp;
import net.catenoid.watcher.uploadImp.KusUploadServiceImp;

public class KusUtilsTest {
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private WatcherFolder[] watchers = null;	
	private Config conf = null;
	private String check_time = null;
	private KusUploadUtils utils = null;
	
	@Before
	public void initSetup() throws Exception {
		this.conf = Config.getConfig();
		this.watchers = conf.getWatchers();
		
		for(WatcherFolder info : watchers) {
//			KusUploadService service = new KusUploadServiceImp(info, conf);
			utils = new KusUploadUtils(info, conf);
		}
		
		Date now = new Date();
		this.check_time = dateFormat.format(now);
		
//		Calendar cal = Calendar.getInstance();
//		cal.add(Calendar.HOUR, -1);
	}
	
	@Test
	public void fileCheck() {
		try {
			String path = "/Users/kollus/http_upload/deuksoo/dummy-file.mp4";
			boolean ok = utils.isCompleteFileCopy(path);
			assertTrue(ok);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
