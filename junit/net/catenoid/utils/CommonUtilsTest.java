package net.catenoid.utils;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.upload.utils.CommonUtils;

public class CommonUtilsTest {
	
	private FileItemDTO item = null;
	
	@Before
	public void generateFileItem() {
		item = new FileItemDTO();
		
//		item.set_physical_path("/Users/kollus/upload/deuksoo/test.mp4");
	}
	
	@Test
	public void MacMediaInfoParseTest() {
//		boolean issucces = item.get_media_info();
//		assertTrue(issucces);
	}
	
	@Test
	public void AddPassthroughTest() {
		Map<String, Object> r = CommonUtils.pathToTRmpk("/home/kollus/http_upload_passthrough/deuksoo-dev/_passthrough/_None/Zedd, Elley Duhe - Happy Now (Official Music Video)_deuksoo-dev-pc1-high+deuksoo-dev-tablet2-high+deuksoo-dev-mobile1-normal.mp4");
		System.out.println(r);
	}
}
