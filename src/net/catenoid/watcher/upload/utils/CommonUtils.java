package net.catenoid.watcher.upload.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.kollus.json_data.BaseCommand;
import com.kollus.json_data.config.ModuleConfig.URLS;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.job.Role_Watcher.PARAM;
import net.catenoid.watcher.upload.dto.KollusApiWatchersDTO;
import net.catenoid.watcher.upload.dto.KollusApiWatcherContentDTO;
import net.catenoid.watcher.upload.dto.KollusApiWatcherDTO;
import net.catenoid.watcher.upload.config.StreamKind;
import net.catenoid.watcher.upload.dto.ContentInfoDTO;
import net.catenoid.watcher.upload.dto.ExtFilenameParserDTO;
import net.catenoid.watcher.upload.dto.ExtFilenameResultDTO;
import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.upload.dto.SendFileItemsDTO;
import net.catenoid.watcher.utils.HttpAgent;
import net.catenoid.watcher.utils.WatcherUtils;

public class CommonUtils {
	private static Logger log = Logger.getLogger(CommonUtils.class);

	/**
	 * HTTP ????????? ????????? ?????? ????????? CHARSET ?????? (UTF-8)
	 */
	protected static String DEFAULT_CHARSET = "UTF-8";

	// WatcherFile ??????
	public WatcherFolder info;
	// Config ??????
	public Config conf;

	public CommonUtils() {
	}

	public CommonUtils(WatcherFolder watcherFolder, Config config) {
		this.info = watcherFolder;
		this.conf = config;
	}

	/*
	 * POST??? API ?????????????????? ??????
	 */

	public String sendPostToApi(SendFileItemsDTO sendItem, String apiType) throws Exception {
		HttpPost httpPost = null;
		HttpResponse response = null;
		Map<String, Object> httpMap = null;
		List<NameValuePair> qparams = null;
		InputStream inputStream = null;
		String responseBody = "";
		int nStatus = 0;

		String url = apiType == "register" ? conf.get_kollus_api().get_url(URLS.WATCHER_LIST_INSERT)
				: conf.get_kollus_api().get_url(URLS.WATCHER_LIST_COMPLETE);
		try {
			// Body Param??? ?????????
			qparams = getHttpQueryParam(sendItem, apiType);

			httpMap = getHttpResponse(url, httpPost, response, qparams);

			httpPost = (HttpPost) httpMap.get("httpPost");
			response = (HttpResponse) httpMap.get("res");

			if (response == null) {
				log.error(LogAction.HTTP_LOG + url + ", status: connection failed");
				transmitErrorReport(503, URLS.WATCHER_LIST_DELETE, "", conf);
			} else {
				Map<String, Object> resMap = getResponseBody(inputStream, response);

				inputStream = (InputStream) resMap.get("is");
				responseBody = (String) resMap.get("res");
				nStatus = (int) resMap.get("status");

				log.debug("Post Response Transfer Content [" + apiType + "] : " + responseBody.toString());

				if (nStatus != 200) {
					transmitErrorReport(nStatus, URLS.WATCHER_LIST_DELETE, "", conf);
					log.error(url + "(" + nStatus + ") : " + responseBody);
					return null;
				}
			}
		} catch (Exception e) {
			log.error(WatcherUtils.getStackTrace(e));
			log.error(responseBody);
			String msgType = apiType == "register" ? URLS.WATCHER_LIST_INSERT : URLS.WATCHER_LIST_COMPLETE;
			transmitErrorReport(5002, "JSON Error: " + msgType, "", conf);
			return null;

		} finally {
			if (inputStream != null)
				inputStream.close();
			if (httpPost != null)
				httpPost.abort();
		}

		if (httpPost != null) {
			httpPost.abort();
		}

		return responseBody;
	}

	/*
	 * API ?????????????????? ????????? Params??? ???????????? ?????? ??????
	 */

	private List<NameValuePair> getHttpQueryParam(SendFileItemsDTO sendItem, String type) throws Exception {
		// Body Param??? ?????????
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		String jsonStr = "";

		switch (type) {
		case "register":
			qparams.add(new BasicNameValuePair(PARAM.API_KEY, info.getApiKey()));
			qparams.add(new BasicNameValuePair(PARAM.API_REFERENCE, info.getApiReference()));

			jsonStr = sendItem.toString(info.getName(), info.getWatcherFileKind());
			qparams.add(new BasicNameValuePair(PARAM.WATCHER_FILES, jsonStr));
			break;
		case "copy":
			qparams.add(new BasicNameValuePair(PARAM.API_KEY, info.getApiKey()));
			qparams.add(new BasicNameValuePair(PARAM.API_REFERENCE, info.getApiReference()));

			jsonStr = sendItem.toString(info.getWatcherFileKind());
			qparams.add(new BasicNameValuePair(PARAM.WATCHER_FILES, jsonStr));

			break;
		}

		log.debug(jsonStr);

		return qparams;
	}

	/*
	 * Http ??????????????? ?????? ??????????????? ???????????? ?????????????????? ??????????????? ???????????? ?????????
	 */
	private Map<String, Object> getHttpResponse(String url, HttpPost httpPost, HttpResponse response,
			List<NameValuePair> qparams) throws Exception {
		Map<String, Object> httpMap = new HashMap<String, Object>();

		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3 * 1000).build();
		HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(qparams, DEFAULT_CHARSET);
		for (int i = 0; i < 3; i += 1) {
			httpPost = new HttpPost(url);
			httpPost.setEntity(entity);
			log.debug("Post Requests Transfer Content : " + qparams.get(qparams.size() - 1).toString());
			// Post Api ??????
			response = httpClient.execute(httpPost);

			if (response != null) {
				break;
			}
			Thread.sleep(1000);
		}
		httpMap.put("res", response);
		httpMap.put("httpPost", httpPost);

		return httpMap;
	}

	/*
	 * BadNetWork Gat ?????? Bad Requests??? ?????? api ????????? ????????? ??????????????? Error??? ?????? api ????????? ??????
	 */
	private void transmitErrorReport(int i_err, String n_msg, String content_provider_key, Config conf) {
		String url = conf.get_kollus_api().get_url(URLS.MODULE_ERROR_REPORT);

		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3 * 1000).build();
		HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

		String n_err = String.valueOf(i_err);
		String main_node = conf.get_kollus_api().get_main_node_key();

		URIBuilder builder = null;
		try {
			builder = new URIBuilder(url);
			builder.setParameter(PARAM.MODULE_TAG, "watcher");
			builder.setParameter(PARAM.ERROR_CODE, n_err);
			builder.setParameter(PARAM.MESSAGE, n_msg);
			builder.setParameter(PARAM.MAIN_NODE_KEY, main_node);
			builder.setParameter(PARAM.CONTENT_PROVIDER_KEY, content_provider_key);
		} catch (URISyntaxException e) {
			log.error(e.toString());
		}

		if (builder != null) {
			URI uri = null;
			try {
				uri = builder.build();
			} catch (URISyntaxException e) {
				log.error(e.toString());
			}

			if (uri != null) {
				HttpGet httpGet = new HttpGet(uri);
				try {
					httpClient.execute(httpGet);
				} catch (ClientProtocolException e) {
					log.error(e.toString());
				} catch (IOException e) {
					log.error(e.toString());
				}

				log.debug(uri.toString());
				httpGet.abort();
			}
		}
	}

	/*
	 * ResponseBod??? Api ?????? Code??? ?????????????????? ??????
	 */
	private Map<String, Object> getResponseBody(InputStream inputStream, HttpResponse response) throws Exception {
		Map<String, Object> httpMap = new HashMap<String, Object>();

		int nStatus = response.getStatusLine().getStatusCode();

		final String charset = HttpAgent.getContentCharSet("UTF-8", response.getEntity());
		inputStream = response.getEntity().getContent();

		String responseBody = HttpAgent.generateString(inputStream, charset);

		httpMap.put("is", inputStream);
		httpMap.put("res", responseBody);
		httpMap.put("status", nStatus);

		return httpMap;

	}

	public FileItemDTO convertResultApiItem(KollusApiWatcherContentDTO item) {
		Gson gson = BaseCommand.gson(false);
		String json = gson.toJson(item);
		log.debug("register ????????? (fileItem) = " + json);
		KollusApiWatcherDTO r = gson.fromJson(json, KollusApiWatcherDTO.class);
		return r.result;
	}

	/**
	 * ??????????????? ????????? ????????????.
	 * 
	 * @param i_err
	 * @param n_msg
	 * @param content_provider_key
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void sendErrorReport(int i_err, String n_msg, String content_provider_key) {

		String url = conf.get_kollus_api().get_url(URLS.MODULE_ERROR_REPORT);
		// HttpClient httpClient = agent.newHttpClient();
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3 * 1000).build();
		HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

		String n_err = String.valueOf(i_err);
		String main_node = conf.get_kollus_api().get_main_node_key();

		URIBuilder builder = null;
		try {
			builder = new URIBuilder(url);
			builder.setParameter(PARAM.MODULE_TAG, "watcher");
			builder.setParameter(PARAM.ERROR_CODE, n_err);
			builder.setParameter(PARAM.MESSAGE, n_msg);
			builder.setParameter(PARAM.MAIN_NODE_KEY, main_node);
			builder.setParameter(PARAM.CONTENT_PROVIDER_KEY, content_provider_key);
		} catch (URISyntaxException e) {
			log.error(e.toString());
		}

		if (builder != null) {
			URI uri = null;
			try {
				uri = builder.build();
			} catch (URISyntaxException e) {
				log.error(e.toString());
			}

			if (uri != null) {
				HttpGet httpGet = new HttpGet(uri);
				try {
					httpClient.execute(httpGet);
				} catch (ClientProtocolException e) {
					log.error(e.toString());
				} catch (IOException e) {
					log.error(e.toString());
				}

				log.debug(uri.toString());
				httpGet.abort();
			}
		}
	}

	public void ExceptionLogPrint(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();
		log.error(exceptionAsString);
		log.error(e.toString());
	}
	
	public static FileItemDTO fromResultSet(ResultSet results, String watcher_dir, String filename, long lastModified,
			long length, String uploadFileKey) throws Exception {
		String physical_path;
		long new_size;
		String key;
		String content_path;
		String content_provider_key;
		String upload_path;
		String snapshot_path;
		String md5;
		int checksum_type;
		int poster_position;
		int poster_width;
		int poster_height;

		FileItemDTO item;

		if (results != null) {
			physical_path = results.getString("PATH");
			new_size = results.getLong("NEW_SIZE");
			key = results.getString("CID");
			content_path = results.getString("CPATH");
			content_provider_key = results.getString("DOMAIN");
			upload_path = results.getString("UPLOAD_PATH");
			snapshot_path = results.getString("SNAP_PATH");
			md5 = results.getString("MD5"); // MD5 ??????
			checksum_type = results.getInt("CHECKSUM_TYPE"); // CHECKSUM_TYPE ??????
			poster_position = results.getInt("POSTER_POS");
			poster_width = results.getInt("POSTER_WIDTH");
			poster_height = results.getInt("POSTER_HEIGHT");
			item = new FileItemDTO();
		} else {
			item = CommonUtils.fromLineParser(watcher_dir, filename, lastModified, length);
			physical_path = item.getPhysicalPath();
			new_size = item.getFilesize();
			key = uploadFileKey != null ? uploadFileKey : "";
			content_path = "";
			content_provider_key = item.getContentProviderKey();
			upload_path = item.getUploadPath();
			snapshot_path = "";
			md5 = ""; // MD5 ??????
			checksum_type = 0; // CHECKSUM_TYPE ??????
			poster_position = item.getPoster() == null ? -1 : item.getPoster().getPosition();
			poster_width = item.getPoster() == null ? -1 : item.getPoster().getWidth();
			poster_height = item.getPoster() == null ? -1 : item.getPoster().getHeight();
		}
		
		ExtFilenameResultDTO getItem = ExtFilenameParserDTO.newParser(upload_path);

		item.setPhysicalPath(physical_path);
		item.setFilesize(new_size);
		item.setUploadFileKey(key);
		item.setContentPath(content_path);
		item.setContentProviderKey(content_provider_key);
		item.setSnapshotPath(snapshot_path);
		item.setMd5(md5);
		item.setChecksumType(checksum_type);
		item.setPoster(new Poster(poster_position, poster_width, poster_height));
		item.setCategory(getItem.getCategory());
		item.setMediaProfileKey(getItem.getMediaProfileKey());

		// passthrough upload?????? ?????????????????????
		item.setTrMediaProfileKey(null);

		log.debug(physical_path);

		// ?????? ??????????????? ??? ??? ?????? Passthrough ??????
		Map<String, Object> addTransCodingInfo = CommonUtils.pathToTRmpk(physical_path);

		if (physical_path != null && addTransCodingInfo != null) {
			item.setTrMediaProfileKey((String) addTransCodingInfo.get("tr_media_profile_key"));
			item.setPassthrough_ahead((int) addTransCodingInfo.get("passthrough_ahead"));
		}

		item.setTitle(getItem.getTitle());
		item.setUploadPath(getItem.getUploadPath());
		
		// Channel ??????
		Map<String,String> forMatMap= getChannels(item.getCategory(), item.getUploadPath());
		
		if(forMatMap != null) {
			item.setChannelKeys(forMatMap.get("channelKeys"));
			item.setCategory(forMatMap.get("categorys"));
			item.setUploadPath(forMatMap.get("uploadPath"));
		}


		if (item.getPassthrough_ahead() == 1) {
			String reuploadPath = CommonUtils.formatAheadUploadPath(item.getUploadPath());
			if (reuploadPath != null) {
				item.setUploadPath(reuploadPath);
			}
		}

		item.setUseEncryption(getItem.isEncryptPath() ? 1 : 0);
		item.setIsAudioFile(getItem.isAudioPath() ? 1 : 0);

		if (item.getPhysicalPath().contains("passthrough")) {
			item.setWatcherFileKind(FileItemDTO.KIND_PASSTHROUGH);
		}

		log.debug("fromResultSet Add TransCoder : " + item.getTrMediaProfileKey());
		log.debug("fromResultSet Path : " + item.getUploadPath());
		log.debug("fromResultSet item : " + item.toString());

		return item;
	}

	/**
	 * SNAP ????????? ????????????.<br>
	 * snapshot??? target??? ?????? ????????????.<br>
	 * snapshot path??? "" ?????? snapshot snapshot??? ???????????? ?????????.<br>
	 * 
	 * @param items
	 */
	public void createSnapFile(ArrayList<FileItemDTO> items) throws Exception {
		for (FileItemDTO item : items) {
			if (item.isExistPhysicalPath() == false) {
				continue;
			}

			/**
			 * snapshot path??? "" ?????? snapshot ????????????????????? ????????????.
			 */
			if (WatcherUtils.isEmpty(item.getSnapshotPath())) {
				item.setSnapshotPath("");
				continue;
			}

			if (item.getMediaInfo() == null) {
				log.warn("mediaInfo == null");
				if (!getMediaContentInfo(item)) {
					// media info??? ???????????? ????????? ??????????????? ??????.
					item.setSnapshotPath("");
					continue;
				}
				log.info("Success to got mediainfo : " + item.toString());
			}

			if (checkSupportExt(item)) {
				/**
				 * ???????????? ?????? ??????????????? ????????? ?????? ??????
				 */
				continue;
			}

			if (WatcherUtils.isEmpty(item.getMediaInfo().getFormat())) {
				log.error("Utils.isEmpty(mediaInfo.format)");
				// media info??? ???????????? ????????? ??????????????? ??????.
				item.setSnapshotPath("");
				continue;
			}

			if (!WatcherUtils.isEmpty(item.getMediaInfo().getVideoFormat())) {
				// ????????? ??????
				SnapCreator snap = new SnapCreator(conf, "", item);

				if (snap.run(0) == false) {
					snap.run(1);
				}

				/*
				 * ????????? ?????? 777 ??? ???????????? ????????? nginx ?????? ????????? watcher ?????? snapshot ?????? ????????? 777 ?????????.
				 */
				String destPath = conf.getSnap().getSnapDir() + item.getSnapshotPath();

				// ?????? ?????? ?????? ??????
				destPath = WatcherUtils.FilenameDefence(destPath);

				/**
				 * ?????? Poster??? ?????????????????? ???????????? ?????? ?????? ?????? path??? '' (Zero-string)?????? ?????????.
				 */
				File posterFile = new File(destPath);
				if (!posterFile.exists()) {
					item.setSnapshotPath("");
					continue;
				}

				// CMS?????? snapshot??? ????????? ??? ????????? ????????????.
				WatcherUtils.chmod777(destPath);
				
				continue;
			} else if (WatcherUtils.isEmpty(item.getMediaInfo().getAudioFormat())) {
				item.setSnapshotPath("");
				continue;
			} else if (WatcherUtils.isEmpty(item.getMediaInfo().getImageFormat())) {
				item.setSnapshotPath("");
				continue;
			}
			
			item.setSnapshotPath("");
			/**
			 * ???????????? ?????? ????????? ?????? SupportExt??? ???????????? ?????? ?????????
			 */
			log.error("unsupport media file: " + item.getPhysicalPath());
		}
	}

	/**
	 * FileItem??? ContentInfo????????? ????????????. ???????????? ?????? mediaInfo??? null ?????? ?????????.
	 * 
	 * @return
	 */
	public boolean getMediaContentInfo(FileItemDTO f) {
		if (f.getMediaInfo() == null) {
			f.setMediaInfo(new ContentInfoDTO());
		}
		boolean bRet = setMediaInfo(f);

		if (bRet == false) {
			f.setMediaInfo(null);
		} else {
			f.format = f.getMediaInfo().getFormat();
		}

		return bRet;
	}

	private boolean setMediaInfo(FileItemDTO f) {
		MediaInfo MI = new MediaInfo();

		if (MI.Open(f.getPhysicalPath()) == 0) {
			log.error("MI.Open - file was not not opened: " + f.getPhysicalPath());
			return false;
		}

		MI.Option("Complete", "");

		f.getMediaInfo().setFormat(empty2null(MI.Get(StreamKind.General, 0, "Format")));
		f.getMediaInfo().setDuration(empty2null(MI.Get(StreamKind.General, 0, "Duration")));
		f.getMediaInfo().setVideoDuration(empty2null(MI.Get(StreamKind.Video, 0, "Duration")));
		f.getMediaInfo().setVideoFormat(empty2null(MI.Get(StreamKind.Video, 0, "Format")));
		f.getMediaInfo().setVideoCodec(empty2null(MI.Get(StreamKind.Video, 0, "CodecID/Hint")));
		f.getMediaInfo().setVideoBitrate(empty2null(MI.Get(StreamKind.Video, 0, "BitRate")));
		f.getMediaInfo().setVideoWidth(empty2null(MI.Get(StreamKind.Video, 0, "Width")));
		f.getMediaInfo().setVideoHeight(empty2null(MI.Get(StreamKind.Video, 0, "Height")));
		f.getMediaInfo().setVideoFrameRate(empty2null(MI.Get(StreamKind.Video, 0, "FrameRate")));
		f.getMediaInfo().setVideoRatio(empty2null(MI.Get(StreamKind.Video, 0, "DisplayAspectRatio")));
		f.getMediaInfo().setRotation(empty2null(MI.Get(StreamKind.Video, 0, "Rotation")));
		f.getMediaInfo().setScanType(empty2null(MI.Get(StreamKind.Video, 0, "ScanType/String")));
		f.getMediaInfo().setAudioFormat(empty2null(MI.Get(StreamKind.Audio, 0, "Format")));
		f.getMediaInfo().setAudioCodec(empty2null(MI.Get(StreamKind.Audio, 0, "CodecID/Hint")));
		f.getMediaInfo().setAudioBitrate(empty2null(MI.Get(StreamKind.Audio, 0, "BitRate")));
		f.getMediaInfo().setAudioSampleRate(empty2null(MI.Get(StreamKind.Audio, 0, "SamplingRate")));
		f.getMediaInfo().setAudioDuration(empty2null(MI.Get(StreamKind.Audio, 0, "Duration")));
		f.getMediaInfo().setImageFormat(empty2null(MI.Get(StreamKind.Image, 0, "Format")));
		f.getMediaInfo().setImageWidth(empty2null(MI.Get(StreamKind.Image, 0, "Width")));
		f.getMediaInfo().setImageHeight(empty2null(MI.Get(StreamKind.Image, 0, "Height")));
		MI.Close();

		log.info("MediaInfo information : " + f.getMediaInformation());
		return true;
	}

	private static String empty2null(String s) {
		if (WatcherUtils.isEmpty(s) == true) {
			return null;
		}
		return s;
	}

	private boolean checkSupportExt(FileItemDTO item) {
		File f = new File(item.getPhysicalPath());
		int idx = f.getName().lastIndexOf('.');
		String ext = ((idx > 0) ? f.getName().substring(idx) : "");

		try {
			for (String i : Config.getConfig().getSupportExt()) {
				if (i.length() > 0 && ext.compareToIgnoreCase(i) == 0) {
					return true;
				}
			}
		} catch (Exception e) {
			log.error(LogAction.CONFIG_ERROR + e.toString());
		}

		return false;
	}

	/**
	 * FileItem??? ???????????? ?????? lineParser?????? ???????????? ???????????????.
	 * 
	 * @param watcher_dir
	 * @param filename
	 * @param lastModified
	 * @param length
	 * @return
	 */
	public static FileItemDTO fromLineParser(String watcher_dir, String filename, long lastModified, long length) {
		FileItemDTO item = new FileItemDTO();
		item.setPhysicalPath(filename);
		item.setFilesize(length);
		item.setLastModified(lastModified);
		item.setContentProviderKey(WatcherUtils.getDomain(watcher_dir, item.getPhysicalPath()));
		item.setUploadPath(WatcherUtils.getUploadPath(watcher_dir, item.getPhysicalPath()));

		String ext = WatcherUtils.getFileExtention(item.getUploadPath());
		item.setTitle(WatcherUtils.getWithoutDomain(watcher_dir, item.getUploadPath()));
		item.setTitle(item.getTitle().substring(0, item.getTitle().length() - ext.length()));
		return item;
	}

	/**
	 * passthrough??? ???????????? ????????????????????? media content key??? ??????????????? stirng parsing
	 * 
	 * @param path
	 * @return
	 */
	public static Map<String, Object> pathToTRmpk(String path) {
		Map<String, Object> result = null;
		
		String TRmpk = null;
		try {
			TRmpk = path.substring(path.lastIndexOf("/")+1);
			log.info("[ pathToTRmpk 1/3 ] "+TRmpk);
			// ????????? ??????. 
			int TRmpkextension = TRmpk.lastIndexOf(".");
			TRmpk = TRmpk.substring(0, TRmpkextension);
			log.info("[ pathToTRmpk 2/3 ] "+TRmpk);
			
			int idx = TRmpk.lastIndexOf("_"); 
			String TRmpkindex = TRmpk.substring(idx+1);
			int idx2 = TRmpkindex.indexOf("+"); 
			TRmpkindex = TRmpkindex.substring(idx2+1);
			// ?????? ??????????????? ??????????????????.
			if(TRmpkindex.isEmpty() || !TRmpk.contains("+")) {
				System.out.println("[ pathToTRmpk 3/3 ] "+TRmpk);
				return null;
			}
			// ?????? ??????????????? parsing
			log.info("[ pathToTRmpk 3/3 ] "+TRmpkindex);
			result = new HashMap<String, Object>();
			
			result.put("tr_media_profile_key", TRmpkindex);
			result.put("passthrough_ahead", 0);
			// Passthrough ????????? Prifix ??????
			int atIdx = TRmpkindex.indexOf("@");
			if (atIdx > -1) {
				// ???????????? ?????? ????????? ????????? ????????????????????? ???????????? ???????????????
				TRmpkindex = TRmpkindex.substring(0, atIdx);
				result.put("tr_media_profile_key", TRmpkindex);
				result.put("passthrough_ahead", 1);
				log.info("[ passthrough_ahead 1/1 ] : "+TRmpkindex);
			}
			
			return result;
		}catch(IndexOutOfBoundsException e) {
			log.error("[ pathToTRmpk 3/3 ] no passthrough ?????? ???????????????. : " + e.getMessage());
		}
		return null;
	}
	
	public static String formatAheadUploadPath(String uploadPath) {
		String reUploadPath = null;
		if (uploadPath == "") {
			return reUploadPath;
		}
		
		String ofileName = uploadPath.substring(uploadPath.lastIndexOf("/")+1);
		String folderPath = uploadPath.substring(0, uploadPath.lastIndexOf(ofileName));
		
		log.debug("[reUploadPath][1/4] OrignalName : " + ofileName + "FolderPath : " + folderPath);
		
		int underBarIdx = ofileName.lastIndexOf("_");
		
		if (underBarIdx == -1) {
			return reUploadPath;
		}
		
		String fileName = ofileName.substring(0, underBarIdx);
		String profiles = ofileName.substring(underBarIdx);
		
		log.debug("[reUploadPath][2/4] Name : " + fileName + "profiles : " + profiles);

		
		profiles = profiles.replaceAll("@", "");
		
		log.debug("[reUploadPath][3/4] profiles : " + profiles);

		
		reUploadPath = folderPath + fileName + profiles;
		
		log.debug("[reUploadPath][4/4] reUploadPath : " + reUploadPath);
		
				
		return reUploadPath;
	}

	/**
	 * ??????????????? ??????????????? ?????? ??????
	 * 
	 * @param items
	 * @return
	 * @throws Exception
	 */
	public int moveToWorkDir(ArrayList<FileItemDTO> items) throws Exception {
		int fileCnt = 0;
		for (FileItemDTO item : items) {
			Path src = Paths.get(item.getPhysicalPath());
			if (!Files.exists(src)) {
				item.setCopyComplete(false);
				continue;
			}

			// ?????????????????? ??????????????? ????????? ???????????? ??????
			String[] parentFolders = (item.getContentPath().split("/"));
			String workDirPath = info.getWorkDir();

			for (String folder : parentFolders) {
				if (!folder.contains(".") && folder.length() > 0) {
					workDirPath += "/" + folder;
				}
			}

			// ??????????????? ?????? ????????? ????????? ???????????? ??????
			Path dstDir = Paths.get(workDirPath);
			if (!Files.isDirectory(dstDir)) {

				Files.createDirectories(dstDir);
			}

			String workFilePath = info.getWorkDir() + item.getContentPath();
			Path dst = Paths.get(workFilePath);

			Path result = Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);

			String completePath = item.getPhysicalPath() + "_complete";
			Path completeSrc = Paths.get(completePath);
			if(Files.deleteIfExists(completeSrc)) {
				log.info("completeFile is deleted by KusUpload created file");
			}
			
			if (Files.isReadable(result)) {
				item.setCopyComplete(true);
				fileCnt += 1;
				continue;
			}
			
		}

		return fileCnt;
	}

	/**
	 * console ???????????? ??????
	 * 
	 * @param fileName
	 * @return
	 */
	protected boolean fileNamePatternCheck(String fileName) {
		fileName = fileName.replaceAll("_complete", "");

		String consoleFilePattern = "^\\d{8}-\\w{8}.\\w{3}$";

		return fileName.matches(consoleFilePattern);
	}

	/**
	 * Kollus Api ????????? Api ???????????? ?????? ?????? ????????? ????????? ??????????????? ?????? ???????????? ?????? NULL ???????????????
	 * 
	 * @param apiResult
	 * @param item
	 */
	public void failApiResultOrRegisterProcess(KollusApiWatchersDTO apiResult, KollusApiWatcherContentDTO item) {
		if (apiResult != null) {
			if (apiResult.result != null && apiResult.result.error_code > 0) {
				sendErrorReport(apiResult.result.error_code, apiResult.message, "");
			}
			return;
		} else if (item != null) {
			if (item.result == null) {
				log.warn("item result is null");
				return;
			}

			if (item.result.deleted_watcher_file_upload_url == null) {
				log.warn("?????? ????????? ?????? Watcher?????? ????????????");
				return;
			}

			String deleted_watcher_file_url = item.result.deleted_watcher_file_upload_url;
			log.warn("?????? ????????? ?????? Watcher?????? ??????: " + deleted_watcher_file_url);

			if (deleted_watcher_file_url.indexOf(info.getWatcherDir()) > 0) {
				log.error("?????? ?????? ????????? watcherDir??? ?????? : " + deleted_watcher_file_url);
				return;
			}

			String watcher_filepath = String.format("%s", deleted_watcher_file_url);
			File watcherFile = new File(watcher_filepath);
			watcherFile.delete();

			if (!watcherFile.exists()) {
				log.info("????????? ?????? ??????: " + watcher_filepath);
				return;
			}

			log.error("????????? ?????? ??????????????? ????????? ?????? ?????????: " + watcher_filepath);
		}
	}

	/**
	 * sendItem??? ?????? FileImte??? ????????? key??? ????????? ?????? FileItem??? ????????????.<br>
	 * ??????????????? null??? ????????????.
	 * 
	 * @param sendItem
	 * @param key
	 * @return FileItem or null
	 */
	public FileItemDTO findSendItem(SendFileItemsDTO sendItem, String key) {
		for (FileItemDTO item : sendItem) {
			if (item.getUploadFileKey().compareTo(key) == 0) {
				return item;
			}
		}
		return null;
	}
	/**
	 * ?????? ?????? Flag ??????
	 * ?????? ????????????????????? "@@@" flag??? ????????? Catenogry ????????? ????????? ?????????
	 * ?????? ???????????? ??? ????????? ?????? "@@@"flag ?????? ??? ???????????? ??????
	 * @param category
	 * @param uploadPath
	 * @return Map<String, String> or null
	 */
	
	public static Map<String, String> getChannels(String category, String uploadPath) {
		// TODO Auto-generated method stub
		Map<String, String> m = null;
		String[] folders = category.split("/");
		int atIdx = folders[folders.length - 1].lastIndexOf("@@@");
		if (atIdx == -1) {
			return m;
		}

		m = new HashMap<String, String>();

		String channelKeys = folders[folders.length - 1].substring(atIdx + 3);
		log.debug("[reCategory - Channel][1/1] channelKeys : " + channelKeys);
		m.put("channelKeys", channelKeys);
		
		category = category.replace("@@@" + channelKeys, "");
		log.debug("[reCategory - Category][1/1] category : " + category);
		m.put("categorys", category);
		
		String ofileName = uploadPath.substring(uploadPath.lastIndexOf("/") + 1);
		String folderPath = uploadPath.substring(0, uploadPath.lastIndexOf(ofileName));

		List<String> l = Arrays.asList(folderPath.split("/"));
		log.debug("[reCategory - UploadPath][1/3] OrignalName : " + ofileName + " UploadPath: " + folderPath);
		String lastFolder = l.get(l.size() - 1);

		int cIdx = lastFolder.lastIndexOf("@@@");
		log.debug("[reCategory - UploadPath][2/3] OrignalName : " + ofileName + " UploadPath: " + folderPath);
		
		lastFolder = lastFolder.substring(0, cIdx);
		l.set(l.size() - 1, lastFolder);
		
		folderPath = "";
		for(String f : l) {
			folderPath += f.concat("/"); 
		}
		
		log.debug("[reCategory - UploadPath][3/3] OrignalName : " + ofileName + " UploadPath: " + folderPath);
		
		m.put("uploadPath", folderPath + ofileName);
		
		return m;
	}


//	public void MacMediaInfoParse(FileItem f) {
//		MediaInfo info = new MediaInfo();
//		
//        String cmd = String.format("/usr/local/bin/mediainfo %s", f.get_physical_path());
//		String s;
//        Process p;
//		try {
//			
//			p = Runtime.getRuntime().exec(cmd);
//			 BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
//	            while ((s = br.readLine()) != null) {
//	            	s = s.replaceAll(" ", "");
//	            	System.out.println(s);
//	            }
//	            p.waitFor();
//	            System.out.println("exit: " + p.exitValue());
//	            p.destroy();
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//
//	}
}
