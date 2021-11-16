package net.catenoid.watcher.upload.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.kollus.json_data.BaseCommand;
import com.kollus.json_data.config.ModuleConfig.URLS;
import com.kollus.utils.Utils;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.job.Role_Watcher.PARAM;
import net.catenoid.watcher.upload.dto.KollusApiWatchersDTO;
import net.catenoid.watcher.upload.dto.KollusApiWatcherContentDTO;
import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.upload.dto.SendFileItemsDTO;
import net.catenoid.watcher.utils.HttpAgent;

public class FtpUploadUtils extends CommonUtils {
	private static Logger log = Logger.getLogger(FtpUploadUtils.class);
//	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private static final int H2_TRACE_LEVEL = 0;

	public FtpUploadUtils() {
	}

	// 공통 Utils 생성자로 전역변수 정의
	public FtpUploadUtils(WatcherFolder info, Config conf) {
		super(info, conf);
	}

	public WatcherFolder getInfo() {
		return this.info;
	}

	public Config getConf() {
		return this.conf;
	}

	/**
	 * 데이터베이스 연결 종료
	 * 
	 * @param jobName
	 * @param stmt
	 * @throws SQLException
	 */
	public void shutdownDerby(Connection conn, Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				log.error(e.toString());
				return;
			}
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
			}
			conn = null;
		}
	}

	/**
	 * Database 연결 statment를 얻는다. 연결풀을 사용할때를 대비해 파라미터를 받음.
	 * 
	 * @param name
	 * @param h2Conn
	 * @return Statement
	 * @throws SQLException
	 */
	public Statement getStatment(Connection conn) throws SQLException {
		Statement stmt = conn.createStatement();
		return stmt;
	}

	/**
	 * 인코딩 취소 : error 관련해서 file , snapshot 삭제 처리
	 * 
	 */
	public void transcodeCancel(FileItemDTO item) {

		String destPath = String.format("%s/%s", info.getWorkDir(), item.getContentPath());
		File mediaFile = new File(destPath);
		if (mediaFile.delete()) {
			log.info("Succeeded to delete a media file");
		} else {
			log.error("failed to delete a media file");
		}

		String snapPath = String.format("%s/%s", conf.getSnap().getSnapDir(), item.getSnapshotPath());
		File snapFile = new File(snapPath);
		if (snapFile.delete()) {
			log.info("Succeeded to delete a snapfile");
		} else {
			log.error("failed to delete a snap file!");
		}

	}

	/**
	 * 파일이 설정에 포함된 무시 파일인지 확인하는 함수 무시 파일이면 true 반환 FileItem.pysical_path을 비교함
	 * 
	 * @param f
	 * @return 무시파일이면 true
	 */

	public boolean isIgnoreFile(FileItemDTO f) {
		for (String ignoreName : conf.getIgnoreFilename()) {
			String temp = f.getPhysicalPath().toLowerCase();
			String ignore = ignoreName.toLowerCase();
			if (temp.startsWith(ignore) == true) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 삭제 파일 리스트를 서버에 전송한다. TODO : 서버에서 삭제되었다고 전송받은 파일 리스트만 삭제할것인가?
	 * 
	 * @param sendItem
	 * @return
	 */
	public boolean sendDeleteFileList(SendFileItemsDTO sendItem) {

		// kind == 2 (http_upload)의 경우 서버에 삭제 정보를 전송하지 않는다.
		if (info.getWatcherFileKind() == 2)
			return true;

		String url = conf.get_kollus_api().get_url(URLS.WATCHER_LIST_DELETE);

		try {
			if (postRemoveFileItem(url, sendItem) == 200) {
				return true;
			}
		} catch (ClientProtocolException e) {
			log.error(e.toString());
		} catch (IOException e) {
			log.error(e.toString());
		}

		return false;
	}

	/**
	 * 감시하던 파일이 리스트에서 제거되면 CMS에 통보한다.
	 * 
	 * @param url
	 * @param sendItem
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ClientProtocolException, IOException
	 */
	private int postRemoveFileItem(String url, SendFileItemsDTO sendItem) throws ClientProtocolException, IOException {

		boolean isConnected = false;
		HttpPost httpPost = null;
		HttpResponse response = null;

		// 2017. 08. 22 재시도 로직 추가(KWCHO)
		for (int i = 0; i < 3; i++) {
			// HttpClient httpClient = agent.newHttpClient();
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3 * 1000).build();
			HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

			List<NameValuePair> qparams = new ArrayList<NameValuePair>();
			qparams.add(new BasicNameValuePair(PARAM.API_KEY, info.getApiKey()));
			qparams.add(new BasicNameValuePair(PARAM.API_REFERENCE, info.getApiReference()));
			qparams.add(new BasicNameValuePair(PARAM.WATCHER_FILES, sendItem.toString(info.getWatcherFileKind())));

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(qparams, DEFAULT_CHARSET);

			httpPost = new HttpPost(url);
			httpPost.setEntity(entity);
			try {
				response = httpClient.execute(httpPost);
				isConnected = true;
			} catch (IOException e) {
				log.error(Utils.getStackTrace(e));
			} catch (Exception e) {
				log.error(Utils.getStackTrace(e));
			}

			if (isConnected)
				break;
			else
				log.debug(String.format("postRemoveFileItem CMS API(%s) connection retry(%d)", url, i + 1));

			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}

		if (!isConnected || response == null) {
			log.error(LogAction.HTTP_LOG + url + ", status: connection failed");
			this.sendErrorReport(503, URLS.WATCHER_LIST_DELETE, "");
			return 503;
		}

		String responseBody = "";
		InputStream inputStream = null;
		int nStatus = response.getStatusLine().getStatusCode();
		try {
			final String charset = HttpAgent.getContentCharSet(DEFAULT_CHARSET, response.getEntity());
			inputStream = response.getEntity().getContent();
//			int contentLength = (int) response.getEntity().getContentLength();

			responseBody = HttpAgent.generateString(inputStream, charset);
			// inputStream.close();

			if (nStatus == 200) {
				log.trace(responseBody);

				try {
					Gson gson = BaseCommand.gson(false);
					KollusApiWatchersDTO api_result = gson.fromJson(responseBody, KollusApiWatchersDTO.class);

					if (api_result.error != 0) {
						log.error(api_result.message);

						if (api_result.result != null && api_result.result.error_code > 0) {
							this.sendErrorReport(api_result.result.error_code, api_result.message, "");
						}
					} else {
						for (KollusApiWatcherContentDTO item : api_result.result.watcher_files) {
							if (item.error != 0) {
								log.error(item.message);
								// 실패한 경우 key정보를 확인하고 원복한다.

								if (item.result != null) {
									/**
									 * error_code가 반환되면 추가 처리함.
									 */
									if (item.result.error_code > 0) {
										this.sendErrorReport(item.result.error_code, item.result.error_detail, "");
									}
								}
							}
						}
					}
				} catch (Exception e) {
					log.error(Utils.getStackTrace(e));
					log.error(responseBody);
					this.sendErrorReport(5002, "JSON Error: " + URLS.WATCHER_LIST_COMPLETE, "");
				}
			} else {
				this.sendErrorReport(nStatus, URLS.WATCHER_LIST_DELETE, "");
				log.error(LogAction.HTTP_LOG + url + ", status:" + nStatus);
			}
		} finally {
			if (inputStream != null)
				inputStream.close();
			if (httpPost != null)
				httpPost.abort();
		}
		return nStatus;
	}

	public boolean sendFtpRegisterApi(SendFileItemsDTO sendItem) throws Exception {
		/**
		 * 서버에 전송한 감지 리스트 등록에 성공하면 해당 파일의 STATUS를 1로 변경한다.
		 */
		if (sendItem.size() == 0) {
			return false;
		}

		/**
		 * 서버에 전송한 감지 리스트 등록에 성공하면 해당 파일의 STATUS를 1로 변경한다.
		 */
		log.debug("file register 보내는값 = " + sendItem.toString());

		String responseBody = sendPostToApi(sendItem, "register");
		if (responseBody == null) {
			return false;
		}
		Gson gson = BaseCommand.gson(false);
		KollusApiWatchersDTO apiResult = gson.fromJson(responseBody, KollusApiWatchersDTO.class);

		if (apiResult.error != 0) {
			log.error(responseBody);
			failApiResultOrRegisterProcess(apiResult, null);
			return false;
		}

		for (KollusApiWatcherContentDTO item : apiResult.result.watcher_files) {
			if (item.error == 0) {
				/**
				 * error == 0인 등록에 성공한 파일
				 */
				FileItemDTO f = convertResultApiItem(item);
				sendItem.update(f);
				continue;
			}

			log.error(item.message);
			
			failApiResultOrRegisterProcess(null, item);
		}

		return true;
	}

	/*
	 * Calculate checksum of a File using MD5 algorithm
	 */
	public String get_file_md5(String path) throws Exception {
		String checksum = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
			MessageDigest md = MessageDigest.getInstance("MD5");

			/**
			 * 2017. 09. 08 KW.CHO 파일 전체를 대상으로 hash를 계산할 경우 파일이 대용량(5G이상)이게 되면 hash계산에 너무 많은
			 * 시간이 소요되기 때문에 파일의 일부분만 읽어(Disk IO에 영향이 적은 정도) hash 계산하도록 로직 수정
			 *
			 * 2017. 12. 19 KW.CHO md5 체크하는 로직은 on/off 기능으로 대체
			 */

			byte[] buffer = new byte[1024 * 1024 * 10];
			int numOfBytesRead;

			while ((numOfBytesRead = fis.read(buffer)) > 0) {
				md.update(buffer, 0, numOfBytesRead);
			}

			byte[] hash = md.digest();

			BigInteger bigInt = new BigInteger(1, hash);
			checksum = bigInt.toString(16);
			// Now we need to zero pad it if you actually want the full 32 chars.
			while (checksum.length() < 32) {
				checksum = "0" + checksum;
			}
		} finally {
			fis.close();
		}

		return checksum;
	}


//	/**
//	 * 트랜스코딩 작업폴더에 파일 이동
//	 * @param items
//	 * @return
//	 * @throws Exception
//	 */
//	public int moveToWorkDir(ArrayList<FileItem> items) throws Exception {
//		int fileCnt = 0;
//		for (FileItem item : items) {
//			Path src = Paths.get(item.getPhysicalPath());
//			if (!Files.exists(src)) {
//				item.setCopyComplete(false);
//				continue;
//			}
//			
//			// 트랜스코딩쪽 작업폴더에 폴더가 없을경우 생성
//			String [] parentFolders = (item.getContentPath().split("/"));
//			String workDirPath = info.getWorkDir();
//			
//			for(String folder: parentFolders) {
//				if(!folder.contains(".") && folder.length() > 0) {
//					workDirPath += "/" + folder;
//				}
//			}
//			
//			// 트랜스코딩 작업 폴더에 폴더가 없을경우 생성
//			Path dstDir = Paths.get(workDirPath);
//			if (!Files.isDirectory(dstDir)) {
//
//				Files.createDirectories(dstDir);
//			}
//			
//			String workFilePath = info.getWorkDir() +"/"+ item.getContentPath();
//			Path dst = Paths.get(workFilePath);
//			
//			Path result = Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
//			
//			if (Files.isReadable(result)) {
//				item.setCopyComplete(true);
//				fileCnt += 1;
//				continue;
//			}
//		}
//		
//		return fileCnt;
//	}
	
	/**
	 * 파일 이동 완료후 Kollus Api Complete Api 1첫번째 블럭 응답값처리
	 * @param sendItem
	 * @return
	 * @throws Exception
	 */
	public KollusApiWatchersDTO sendFtpCompleteApi(SendFileItemsDTO sendItem) throws Exception {
		String responseBody = null;
      

		responseBody = sendPostToApi(sendItem, "copy");
			if(responseBody == null) {
				return null;
			}
			
			Gson gson = BaseCommand.gson(false);
			KollusApiWatchersDTO apiResult = gson.fromJson(responseBody, KollusApiWatchersDTO.class);
			
			if (apiResult.error != 0) {
				log.error(responseBody);
				failApiResultOrRegisterProcess(apiResult, null);
				return null;
			}
			
			return apiResult;
	}	
}
