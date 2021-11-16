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
	 * HTTP 통신에 사용될 기본 문자열 CHARSET 설정 (UTF-8)
	 */
	protected static String DEFAULT_CHARSET = "UTF-8";

	// WatcherFile 정보
	public WatcherFolder info;
	// Config 정보
	public Config conf;

	public CommonUtils() {
	}

	public CommonUtils(WatcherFolder watcherFolder, Config config) {
		this.info = watcherFolder;
		this.conf = config;
	}

	/*
	 * POST로 API 전송하기위해 사용
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
			// Body Param을 채우기
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
	 * API 전송하기위해 사용될 Params을 가져오기 위해 사용
	 */

	private List<NameValuePair> getHttpQueryParam(SendFileItemsDTO sendItem, String type) throws Exception {
		// Body Param을 채우기
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
	 * Http 통신연결을 해당 클래스에서 공통으로 사용하기위해 멤버변수로 선언하여 사용중
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
			// Post Api 전송
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
	 * BadNetWork Gat 또는 Bad Requests외 등등 api 전송상 문제가 발생된경우 Error을 받는 api 쪽으로 전송
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
	 * ResponseBod와 Api 응답 Code을 가져오기위해 사용
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
		log.debug("register 반환값 (fileItem) = " + json);
		KollusApiWatcherDTO r = gson.fromJson(json, KollusApiWatcherDTO.class);
		return r.result;
	}

	/**
	 * 에러정보를 서버로 전송한다.
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
			md5 = results.getString("MD5"); // MD5 추가
			checksum_type = results.getInt("CHECKSUM_TYPE"); // CHECKSUM_TYPE 추가
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
			md5 = ""; // MD5 추가
			checksum_type = 0; // CHECKSUM_TYPE 추가
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

		// passthrough upload일때 추가트렌스코딩
		item.setTrMediaProfileKey(null);

		log.debug(physical_path);

		// 추가 트랜스코딩 및 선 진행 Passthrough 확인
		Map<String, Object> addTransCodingInfo = CommonUtils.pathToTRmpk(physical_path);

		if (physical_path != null && addTransCodingInfo != null) {
			item.setTrMediaProfileKey((String) addTransCodingInfo.get("tr_media_profile_key"));
			item.setPassthrough_ahead((int) addTransCodingInfo.get("passthrough_ahead"));
		}

		item.setTitle(getItem.getTitle());
		item.setUploadPath(getItem.getUploadPath());
		
		// Channel 찾기
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
	 * SNAP 파일을 생성한다.<br>
	 * snapshot은 target에 직접 저장된다.<br>
	 * snapshot path가 "" 이면 snapshot snapshot은 저장하지 않는다.<br>
	 * 
	 * @param items
	 */
	public void createSnapFile(ArrayList<FileItemDTO> items) throws Exception {
		for (FileItemDTO item : items) {
			if (item.isExistPhysicalPath() == false) {
				continue;
			}

			/**
			 * snapshot path가 "" 이면 snapshot 찍은것은것으로 판단한다.
			 */
			if (WatcherUtils.isEmpty(item.getSnapshotPath())) {
				item.setSnapshotPath("");
				continue;
			}

			if (item.getMediaInfo() == null) {
				log.warn("mediaInfo == null");
				if (!getMediaContentInfo(item)) {
					// media info를 획득하지 못해도 등록하도록 한다.
					item.setSnapshotPath("");
					continue;
				}
				log.info("Success to got mediainfo : " + item.toString());
			}

			if (checkSupportExt(item)) {
				/**
				 * 추가지원 파일 확장자들은 무조건 성공 반환
				 */
				continue;
			}

			if (WatcherUtils.isEmpty(item.getMediaInfo().getFormat())) {
				log.error("Utils.isEmpty(mediaInfo.format)");
				// media info를 획득하지 못해도 등록하도록 한다.
				item.setSnapshotPath("");
				continue;
			}

			if (!WatcherUtils.isEmpty(item.getMediaInfo().getVideoFormat())) {
				// 썸네일 확인
				SnapCreator snap = new SnapCreator(conf, "", item);

				if (snap.run(0) == false) {
					snap.run(1);
				}

				/*
				 * 스냅삿 권한 777 웹 서버에서 사용자 nginx 되어 있어서 watcher 에서 snapshot 파일 권한을 777 셋팅함.
				 */
				String destPath = conf.getSnap().getSnapDir() + item.getSnapshotPath();

				// 경로 방어 코드 추가
				destPath = WatcherUtils.FilenameDefence(destPath);

				/**
				 * 실제 Poster가 생성되었는지 확인하고 해당 없는 경우 path를 '' (Zero-string)으로 만든다.
				 */
				File posterFile = new File(destPath);
				if (!posterFile.exists()) {
					item.setSnapshotPath("");
					continue;
				}

				// CMS에서 snapshot을 삭제할 수 있도록 요청받음.
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
			 * 지원하지 않는 미디어 파일 SupportExt로 걸러지지 않은 파일임
			 */
			log.error("unsupport media file: " + item.getPhysicalPath());
		}
	}

	/**
	 * FileItem의 ContentInfo정보를 획득한다. 실패하는 경우 mediaInfo는 null 값을 갖는다.
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
	 * FileItem을 생성하는 함수 lineParser에서 사용되는 함수입니다.
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
	 * passthrough로 업로드시 추가트렌스코딩 media content key가 존재한다면 stirng parsing
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
			// 확장자 제거. 
			int TRmpkextension = TRmpk.lastIndexOf(".");
			TRmpk = TRmpk.substring(0, TRmpkextension);
			log.info("[ pathToTRmpk 2/3 ] "+TRmpk);
			
			int idx = TRmpk.lastIndexOf("_"); 
			String TRmpkindex = TRmpk.substring(idx+1);
			int idx2 = TRmpkindex.indexOf("+"); 
			TRmpkindex = TRmpkindex.substring(idx2+1);
			// 추가 트렌스코딩 존재여부확인.
			if(TRmpkindex.isEmpty() || !TRmpk.contains("+")) {
				System.out.println("[ pathToTRmpk 3/3 ] "+TRmpk);
				return null;
			}
			// 추가 트렌스코딩 parsing
			log.info("[ pathToTRmpk 3/3 ] "+TRmpkindex);
			result = new HashMap<String, Object>();
			
			result.put("tr_media_profile_key", TRmpkindex);
			result.put("passthrough_ahead", 0);
			// Passthrough 선진행 Prifix 검사
			int atIdx = TRmpkindex.indexOf("@");
			if (atIdx > -1) {
				// 있을경우 해당 부분을 제거후 추가트랜스코딩 문자열로 변환해야됨
				TRmpkindex = TRmpkindex.substring(0, atIdx);
				result.put("tr_media_profile_key", TRmpkindex);
				result.put("passthrough_ahead", 1);
				log.info("[ passthrough_ahead 1/1 ] : "+TRmpkindex);
			}
			
			return result;
		}catch(IndexOutOfBoundsException e) {
			log.error("[ pathToTRmpk 3/3 ] no passthrough 추가 트렌스코딩. : " + e.getMessage());
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
	 * 트랜스코딩 작업폴더에 파일 이동
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

			// 트랜스코딩쪽 작업폴더에 폴더가 없을경우 생성
			String[] parentFolders = (item.getContentPath().split("/"));
			String workDirPath = info.getWorkDir();

			for (String folder : parentFolders) {
				if (!folder.contains(".") && folder.length() > 0) {
					workDirPath += "/" + folder;
				}
			}

			// 트랜스코딩 작업 폴더에 폴더가 없을경우 생성
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
	 * console 파일인지 확인
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
	 * Kollus Api 호출시 Api 실패건에 대한 처리 반드시 하나의 파라미터만 넣고 그외에는 모두 NULL 처리해야됨
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
				log.warn("서버 요청에 의해 Watcher파일 삭제없음");
				return;
			}

			String deleted_watcher_file_url = item.result.deleted_watcher_file_upload_url;
			log.warn("서버 요청에 의해 Watcher파일 삭제: " + deleted_watcher_file_url);

			if (deleted_watcher_file_url.indexOf(info.getWatcherDir()) > 0) {
				log.error("삭제 경로 시작이 watcherDir이 아님 : " + deleted_watcher_file_url);
				return;
			}

			String watcher_filepath = String.format("%s", deleted_watcher_file_url);
			File watcherFile = new File(watcher_filepath);
			watcherFile.delete();

			if (!watcherFile.exists()) {
				log.info("요청에 의해 삭제: " + watcher_filepath);
				return;
			}

			log.error("요청에 의해 삭제하려고 했으나 삭제 실패함: " + watcher_filepath);
		}
	}

	/**
	 * sendItem에 있는 FileImte에 동일한 key가 있으면 해당 FileItem을 반환한다.<br>
	 * 찾지못하면 null을 반환한다.
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
	 * 채널 생성 Flag 처리
	 * 채널 생성하게될경우 "@@@" flag가 마지막 Catenogry 경로에 붙여서 들어옴
	 * 해당 카테고리 및 업로드 패스 "@@@"flag 제거 및 채널정보 얻기
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
