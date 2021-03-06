package net.catenoid.watcher.files;

import static org.junit.Assert.*;

import org.junit.Test;

public class ExtFilenameParserTest {

	public static void log(String msg) {
		System.out.println(msg);
	}
	
	@Test
	public void testPath0() {
		ExtFilenameResult result = ExtFilenameParser.Parser("/tropian/");
		assertEquals(true, result.isError());
		assertEquals(null, result.getCategory());
		assertEquals(null, result.getContentProviderKey());
		assertEquals(null, result.getExtension());
		assertEquals(null, result.getFilename());
		assertEquals(null, result.getTitle());
		assertEquals(null, result.getUploadPath());
		assertEquals("", result.getMediaProfileKey());
		assertEquals(false, result.isAudioPath());
		assertEquals(false, result.isEncryptPath());
		assertEquals(false, result.isPassthroughPath());
	}
	
	@Test
	public void testPath1() {
		ExtFilenameResult result = ExtFilenameParser.Parser("tropian");
		assertEquals(true, result.isError());
		assertEquals(null, result.getCategory());
		assertEquals(null, result.getContentProviderKey());
		assertEquals(null, result.getExtension());
		assertEquals(null, result.getFilename());
		assertEquals(null, result.getTitle());
		assertEquals(null, result.getUploadPath());
		assertEquals("", result.getMediaProfileKey());
		assertEquals(false, result.isAudioPath());
		assertEquals(false, result.isEncryptPath());
		assertEquals(false, result.isPassthroughPath());
	}
	
	@Test
	public void testPath2() {
		ExtFilenameResult result = ExtFilenameParser.Parser("/tropian");
		assertEquals(true, result.isError());
		assertEquals(null, result.getCategory());
		assertEquals(null, result.getContentProviderKey());
		assertEquals(null, result.getExtension());
		assertEquals(null, result.getFilename());
		assertEquals(null, result.getTitle());
		assertEquals(null, result.getUploadPath());
		assertEquals("", result.getMediaProfileKey());
		assertEquals(false, result.isAudioPath());
		assertEquals(false, result.isEncryptPath());
		assertEquals(false, result.isPassthroughPath());
	}
	
	@Test
	public void testPath3() {
		ExtFilenameResult result = ExtFilenameParser.Parser("/tropian/sistar_tropian-mobile2-high_mp4");
		assertEquals(false, result.isError());
		assertEquals("", result.getCategory());
		assertEquals("tropian", result.getContentProviderKey());
		assertEquals("", result.getExtension());
		assertEquals("sistar_tropian-mobile2-high_mp4", result.getFilename());
		assertEquals("sistar_tropian-mobile2-high_mp4", result.getTitle());
		assertEquals("/tropian/sistar_tropian-mobile2-high_mp4", result.getUploadPath());
		assertEquals(false, result.isAudioPath());
		assertEquals(false, result.isEncryptPath());
		assertEquals(false, result.isPassthroughPath());
	}

	@Test
	public void testPath4() {
		ExtFilenameResult result = ExtFilenameParser.Parser("/tropian/_sistar_tropian-mobile2-high.mp4");
		assertEquals(false, result.isError());
		assertEquals("", result.getCategory());
		assertEquals("tropian", result.getContentProviderKey());
		assertEquals("mp4", result.getExtension());
		assertEquals("_sistar_tropian-mobile2-high.mp4", result.getFilename());
		assertEquals("_sistar_tropian-mobile2-high", result.getTitle());
		assertEquals("/tropian/_sistar_tropian-mobile2-high.mp4", result.getUploadPath());
		assertEquals("", result.getMediaProfileKey());
		assertEquals(false, result.isAudioPath());
		assertEquals(false, result.isEncryptPath());
		assertEquals(false, result.isPassthroughPath());
	}

	@Test
	public void testPath5() {
		ExtFilenameResult parser = ExtFilenameParser.Parser("/tropian/_audio_encrypt/_sistar_tropian-mobile2-high.mp4");
		assertEquals(false, parser.isError());
		assertEquals("", parser.getCategory());
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("_sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("_sistar_tropian-mobile2-high", parser.getTitle());
		assertEquals("/tropian/_sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("", parser.getMediaProfileKey());
		assertEquals(true, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(false, parser.isPassthroughPath());
	}
	
	@Test
	public void testPath6() {
		ExtFilenameResult parser = ExtFilenameParser.Parser("/tropian/_audio_encrypt/_sample/_sistar_tropian-mobile2-high.mp4");
		assertEquals(false, parser.isError());
		assertEquals("sample", parser.getCategory());
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("_sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("_sistar_tropian-mobile2-high", parser.getTitle());
		assertEquals("/tropian/_sample/_sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("", parser.getMediaProfileKey());
		assertEquals(true, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(false, parser.isPassthroughPath());
	}

	@Test
	public void testPath7() {
		String str = "/tropian/_encrypt/_sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("", parser.getCategory());
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("_sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("_sistar_tropian-mobile2-high", parser.getTitle());
		assertEquals("/tropian/_sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(false, parser.isPassthroughPath());
	}

	@Test
	public void testPath8() {
		String str = "/tropian/_passthrough/_sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("", parser.getCategory());
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("_sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("_sistar", parser.getTitle());
		assertEquals("/tropian/_sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(false, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
	}
	
	@Test
	public void testPath9() {
		String str = "/tropian/_passthrough_encrypt/_??????/_???????????/sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("??????/???????????", parser.getCategory());
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("sistar", parser.getTitle());
		assertEquals("/tropian/_??????/_???????????/sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
	}

	@Test
	public void testPath10() {
		String str = "/tropian/_passthrough_encrypt/_??????/_????????? space??/sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("??????/????????? space??", parser.getCategory());
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("sistar", parser.getTitle());
		assertEquals("/tropian/_??????/_????????? space??/sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath11() {
		String str = "/tropian/_passthrough_encrypt/_??????/???????????/sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("??????", parser.getCategory());
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("???????????/sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("???????????/sistar", parser.getTitle());
		assertEquals("/tropian/_??????/???????????/sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath12() {
		String str = "/tropian/_passthrough_encrypt/__??????_/_?????????__/sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("_??????_/?????????__", parser.getCategory());
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("sistar", parser.getTitle());
		assertEquals("/tropian/__??????_/_?????????__/sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath13() {
		String str = "/tropian/_passthrough_encrypt/_??????/???????????/_test/sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("??????", parser.getCategory());		
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("???????????/_test/sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("???????????/_test/sistar", parser.getTitle());
		assertEquals("/tropian/_??????/???????????/_test/sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath14() {
		String str = "/tropian/_passthrough_encrypt/_??????/_?????????/_sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("??????/?????????", parser.getCategory());		
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("_sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("_sistar", parser.getTitle());
		assertEquals("/tropian/_??????/_?????????/_sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath15() {
		String str = "/tropian/_passthrough_encrypt/_??????/_?????????/_????????????/_sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("??????/?????????/????????????", parser.getCategory());		
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("_sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("_sistar", parser.getTitle());
		assertEquals("/tropian/_??????/_?????????/_????????????/_sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath16() {
		String str = "/tropian/_passthrough_encrypt/_??????/_?????????/_????????????/sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("??????/?????????/????????????", parser.getCategory());		
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("sistar", parser.getTitle());
		assertEquals("/tropian/_??????/_?????????/_????????????/sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath17() {
		String str = "/tropian/_audio_passthrough_encrypt/_??????/?????????/_????????????/sistar_tropian-mobile2-high.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("??????", parser.getCategory());		
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("?????????/_????????????/sistar_tropian-mobile2-high.mp4", parser.getFilename());
		assertEquals("?????????/_????????????/sistar", parser.getTitle());
		assertEquals("/tropian/_??????/?????????/_????????????/sistar_tropian-mobile2-high.mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high", parser.getMediaProfileKey());
		assertEquals(true, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath18() {
		String str = "/tropian/_audio_passthrough_encrypt/_??????/?????????/_????????????/sistar_tropian-mobile2-high-mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("??????", parser.getCategory());		
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("", parser.getExtension());
		assertEquals("?????????/_????????????/sistar_tropian-mobile2-high-mp4", parser.getFilename());
		assertEquals("?????????/_????????????/sistar", parser.getTitle());
		assertEquals("/tropian/_??????/?????????/_????????????/sistar_tropian-mobile2-high-mp4", parser.getUploadPath());
		assertEquals("tropian-mobile2-high-mp4", parser.getMediaProfileKey());
		assertEquals(true, parser.isAudioPath());
		assertEquals(true, parser.isEncryptPath());
		assertEquals(true, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath_upload_file_key() {
		String str = "/tropian/_upload_file_key/aabbccdd";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("", parser.getCategory());		
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("", parser.getExtension());
		assertEquals("aabbccdd", parser.getFilename());
		assertEquals("aabbccdd", parser.getTitle());
		assertEquals("/tropian/aabbccdd", parser.getUploadPath());
		assertEquals("", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(false, parser.isEncryptPath());
		assertEquals(false, parser.isPassthroughPath());
		assertEquals(true, parser.isUploadFileKeyPath());
		assertEquals(false, parser.isMediaContentKeyPath());
	}

	@Test
	public void testPath_media_content_key() {
		String str = "/tropian/_media_content_key/aabbccdd.mp4";
		ExtFilenameResult parser = ExtFilenameParser.Parser(str);
		assertEquals(false, parser.isError());
		assertEquals("", parser.getCategory());		
		assertEquals("tropian", parser.getContentProviderKey());
		assertEquals("mp4", parser.getExtension());
		assertEquals("aabbccdd.mp4", parser.getFilename());
		assertEquals("aabbccdd", parser.getTitle());
		assertEquals("/tropian/aabbccdd.mp4", parser.getUploadPath());
		assertEquals("", parser.getMediaProfileKey());
		assertEquals(false, parser.isAudioPath());
		assertEquals(false, parser.isEncryptPath());
		assertEquals(false, parser.isPassthroughPath());
		assertEquals(false, parser.isUploadFileKeyPath());
		assertEquals(true, parser.isMediaContentKeyPath());
	}
}
