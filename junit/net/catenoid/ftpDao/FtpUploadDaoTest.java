package net.catenoid.ftpDao;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.upload.FtpUploadService;
import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.uploadDao.FtpUploadDao;
import net.catenoid.watcher.uploadImp.FtpUploadServiceImp;



public class FtpUploadDaoTest extends FtpUploadDao {
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private WatcherFolder[] watchers = null;	
	private Config conf = null;
	private String check_time = null;
	
	
	@Before
	public void initSetup() throws Exception {
		this.conf = Config.getConfig();
		this.watchers = conf.getWatchers();
		
		for(WatcherFolder info : watchers) {
			FtpUploadService service = new FtpUploadServiceImp(info, conf);
			service.renewWorkFileList();
		}
		
		Date now = new Date();
		this.check_time = dateFormat.format(now);
		
//		Calendar cal = Calendar.getInstance();
//		cal.add(Calendar.HOUR, -1);
	}
	
	@Test
	public void insertFileItemTest() {
		FileItemDTO f = new FileItemDTO();
		
//		f.set_checksum_type(checksum_type);
		
		assertTrue(insertFileItem(f,check_time));
		
	}
	
	@Test
	public void existFileItemTest() throws SQLException {
		FileItemDTO f = new FileItemDTO();

		boolean t = existFileItem(f, check_time);

		assertTrue(t);
	}
	
}
