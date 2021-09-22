package net.catenoid.watcher.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.kollus.utils.Utils;
import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.files.ConvertMove;
import net.catenoid.watcher.files.FileItem;
import net.catenoid.watcher.files.LSParser;
import net.catenoid.watcher.files.FileCopyCommander;
import net.catenoid.watcher.files.SendFileItems;
import net.catenoid.watcher.job.Role_Watcher.ApiResult_WatcherApi.WatcherFile;
import net.catenoid.watcher.utils.HttpAgent;

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
import org.h2.store.fs.FileUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.kollus.json_data.BaseCommand;
import com.kollus.json_data.config.ModuleConfig.URLS;
import com.kollus.utils.myUtils;

@DisallowConcurrentExecution
public class Role_Watcher implements InterruptableJob
{
	private static final int THREAD_POOL = 10;
	private static final int H2_TRACE_LEVEL = 0;
	private static Logger log = Logger.getLogger(Role_Watcher.class);
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	static {
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	//private static String NEW_LINE = System.getProperty("line.separator");
	
	/**
	 * HTTP 통신에 사용될 기본 문자열 CHARSET 설정 (UTF-8)
	 */
	private static String DEFAULT_CHARSET = "UTF-8";
		
	/**
	 * derby 데이터베이스 파일 연결 정보
	 */
	private Connection conn = null;
	private WatcherFolder info = null;	
	private Config conf = null;
	
	/**
	 * Job을 시작한 시간 (dateFormat style에 따른다.)
	 */
	private String check_time = "";
	
	/**
	 * Http 통신용 공용 Agent
	 */
	private HttpAgent agent = new HttpAgent();
	
	public interface JSON {
		public final String ERROR = "error";
		public final String MESSAGE = "message";
		public final String RESULT = "result";
		public final String KEY = "key";
		public final String DELETED_WATCHER_FILE_UPLOAD_URL = "deleted_watcher_file_upload_url";
		public final String CONTENT_PROVIDER_KEY = "content_provider_key";
		public final String ERROR_CODE = "error_code";
		public final String ERROR_DETAIL = "error_detail";
	}
	
	public interface PARAM {
		public final String API_KEY = "api_key";
		public final String API_REFERENCE = "api_reference";
		public final String WATCHER_FILES = "watcher_files";
		
		public final String MODULE_TAG = "n_tag";
		public final String ERROR_CODE = "i_err";
		public final String MESSAGE = "n_msg";
		public final String MAIN_NODE_KEY = "m";
		public final String CONTENT_PROVIDER_KEY = "cpk";
	}
	
	public void run(WatcherFolder wathcerInfo) throws JobExecutionException {
		this.info = wathcerInfo;
		execute(null);
	}

	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {

		/**
		 * 환경설정 정보를 얻는다.
		 * 정보가 잘못된 경우 Exception 발생
		 */
		try {
			conf = Config.getConfig();
		} catch (Exception e) {
			log.error(e.toString());
			return;
		}
		
		/**
		 * 스케쥴에 따라 실행되는 감시 설정 데이터 획득
		 */
		if(ctx != null) {
			info = (WatcherFolder) ctx.getMergedJobDataMap().get("data");
		}

		log.trace("execute : " + info.getName());

		/**
		 * 작업의 시작을 서버에 통보하도록 업데이트 한다.
		 */
		long _chk_time = System.currentTimeMillis();
		// 작업 완료후에 작업시간을 업데이트 함
		//JobStatus.getJobStatus().updateWorkTime(info.getName(), _chk_time, 0);
		
		/**
		 * Job에 필요한 초기화 작업 실행
		 */
		if(initWatcher() == false) {
			log.error("initWatcher Error: " + info.getName());
			return;
		}
		
		/**
		 * Job을 실행한 시작 시간을 문자열로 보관
		 * 동일한 작업시점에 같은 시간을 사용하기 위해서 사용함
		 */
//		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")); 
		
		Date now = new Date();
		check_time = dateFormat.format(now);
				
		try {
			conn = connectDatabase(info.getName());
		} catch (Exception e) {
			log.error(e.toString());
			return;
		}
		
		/**
		 * Database 연결 statment를 얻는다.
		 */
		Statement stmt = null;
		try {
			stmt = getStatment(info.getName(), conn);
		} catch (SQLException e) {
			log.error(e.toString());
			return;
		}
		
		/**
		 * 필요한 테이블을 생성한다.
		 */
		createTable(stmt);
		
		
		/**
		 * 감시 폴더 내용을 DB에 등록한다.
		 */
		ArrayList<FileItem> lsFiles = new ArrayList<FileItem>();
		ArrayList<FileItem> lsDirs = new ArrayList<FileItem>();
		
		newFileItems(stmt, lsDirs, lsFiles);
	
		/**
		 * 삭제된 FileItem을 DB에서 제거한다.
		 */
		try {
			deleteUnknownItem(stmt, check_time);
		} catch (SQLException e) {
			log.error(myUtils.getStackTrace(e));
		}

		/**
		 * 감시대상 파일이 이전 상태와 동일하면 비교회수를 증가시킨다.
		 */
		try {
			updateCompareCount(stmt);
		} catch (SQLException e) {
			log.error(myUtils.getStackTrace(e));
		}
//		selectTable(stmt, "FILES", "PATH");
		/**
		 * 서버에 등록하지 못한 감시 대상을 등록한다.
		 */
//		long _start = System.currentTimeMillis();
		if(sendWatchFiles(stmt) == false) {
			log.error("error sendWatchFiles. : " + info.getName());
		} else {
			log.trace("sendWatchFiles complete : " + info.getName());
		}
//		selectTable(stmt, "FILES", "PATH");
		
		/**
		 * 작업 대상 파일 리스트를 획득한다.
		 */
		ArrayList<FileItem> items = selectCompareOver(stmt, check_time);
		if(items != null) {
			log.trace("selectCompareOver result : " + info.getName() + ", " + items.toString());
			/**
			 * 작업 대상 파일의 SNAP을 생성하고 성공하면 작업 대상 폴더로 이동한다.
			 * WORK 폴더로 이동시킨 파일은 서버로 통보한다. 
			 */
			if(items.size() > 0) {
//				log.debug("createSnapFile : " + info.getName() + ", " + items.size());
				createSnapFile(items);
				if(copyFileItems(stmt, items) > 0) {
					/**
					 * TODO: 작업폴더로 이동시킨 파일의 0개 이상이면 서버로 데이터를 전송한다. 전송을 실패한 경우 원래 폴더로 복귀시킨다.
					 */
					postCopyCompleteFiles(stmt, items);
				}
			} else {
				log.trace("selectCompareOver == item-size: " + items.size() + ", " + info.getName());
			}
		} else {
			log.trace("selectCompareOver == null" + ", " + info.getName());
		}
		
		/**
		 * 빈폴더 삭제
		 * 확장폴더 기능 구현으로 빈폴더 제거 기능 동작하지 않도록 함. (2013-03-04)
		 */
//		removeEmptyFolder(lsDirs);
log.trace("# 1" + ", " + info.getName());
		removeNotExistFile(stmt);
log.trace("# 2" + ", " + info.getName());
				
//		if(log.isDebugEnabled()) {
//			selectTable(stmt, "FILES", "PATH");
//		}
//log.trace("# 3" + ", " + info.getName());
		long _end = System.currentTimeMillis();
		long _elapsedTime = _end - _chk_time;
log.trace("# 4" + ", " + info.getName());
		/**
		 * DB 연결 관련 내용을 정리한다.
		 */
		try {
			shutdownDerby(info.getName(), stmt);
		} catch (Exception e) {
			log.error(e.toString());
			return;
		}		
log.trace("execute End : " + info.getName());
	}

	/**
	 * 비교는 계속하지만 서버에서 등록을 받아주지 않은 컨텐츠 정보를 삭제한다.
	 * 이때 파일이 존재하지 않는 경우만 삭제한다.
	 * @param stmt
	 */
	private void removeNotExistFile(Statement stmt) {

		ArrayList<String> checkFiles = new ArrayList<String>();
		try {
			String sqlFmt = "SELECT PATH, OLD_SIZE, NEW_SIZE, OLD_DATE, NEW_DATE, COMP_CNT, STATUS, CHK_DATE, CID, CPATH, DOMAIN, UPLOAD_PATH, SNAP_PATH, TITLE " +
					" FROM FILES WHERE COMP_CNT > 10 AND CID = ''";			

			String sql = String.format(sqlFmt);
			ResultSet results = stmt.executeQuery(sql);

			while(results.next()) {
				String physical_path = results.getString(1);
				File file = new File(physical_path);
//				log.debug(physical_path + " - " + file.getAbsolutePath());
				if(file.exists() == false) {
					checkFiles.add(physical_path);
				}
			}			
			results.close();
		} catch (SQLException e) {
			log.error(e.toString());
		}

		try {
			for(String item : checkFiles) {		
				String sqlUpdate = String.format("DELETE FROM FILES WHERE PATH='%s' AND COMP_CNT > 10", item);
				
				if(stmt.executeUpdate(sqlUpdate) == 0) {
					log.error(sqlUpdate);
				}
			}
		} catch (SQLException e) {
			log.error(e.toString());
		}
	}

	/**
	 * H2 로컬 DB의 FILES 테이블에 있는 레코드 수를 반환한다.
	 */
	private int getFilesRow(Statement stmt) {
		int rowcount = 0;
		try {
			String sql = "SELECT COUNT(*) FROM FILES";
			ResultSet results = stmt.executeQuery(sql);
			if(results.next()) {
				rowcount = results.getInt(1);
			}
			results.close();
		} catch (SQLException e) {
			log.debug(e.toString());
		}

		return rowcount;
	}

	/**
	 *   인코딩 취소 : error 관련해서  file , snapshot 삭제 처리 
	 *   
	 */
	private void transcodeCancel(FileItem item){
		
		String destPath = String.format("%s/%s", info.getWorkDir(), item.get_content_path());
		File mediaFile = new File(destPath);
		if(mediaFile.delete()){
			log.info("Succeeded to delete a media file");
		}else{
			log.error("failed to delete a media file");
		}
		
		String snapPath = String.format("%s/%s", conf.getSnap().getSnapDir(), item.get_snapshot_path());
		File snapFile = new File(snapPath);
		if(snapFile.delete()){
		   log.info("Succeeded to delete a snapfile");	
		}else{
			log.error("failed to delete a snap file!");
		}
		
	}
	
	/**
	 * 작업폴더로 이동시킨 파일 정보를 서버로 전송한다.<br>
	 * post에 성공하면 파일을 삭제한다.
	 * @param stmt 
	 * @param items
	 * @return
	 */
	private void postCopyCompleteFiles(Statement stmt, ArrayList<FileItem> items) {
		String url = conf.get_kollus_api().get_url(URLS.WATCHER_LIST_COMPLETE);		
		SendFileItems sendItem = new SendFileItems();
		for(FileItem item : items) {
			if(item.is_copy_complete() == true) {
				// CopyComplete 전송 대상 파일들의 STATUS값을 2로 설정한다.
				this.db_update_file_status(stmt, item.get_physical_path(), 2, item.get_upload_file_key(), item.get_content_path(), item.get_snapshot_path(), item.get_checksum_type(), item.getPoster());
				sendItem.add(item);
				log.trace(item.toString());
			} else {
				log.debug("failed copy file item: " + item.toString());
			}
		}
		
		try {
			if(postWatcherFileComplete(stmt, url, sendItem) != 200) {
				for(FileItem item : sendItem) {
					// 오류일 경우 재 전송을 위해 STATUS값을 1로 rollback 시킨다.
					this.db_update_file_status(stmt, item.get_physical_path(), 1, item.get_upload_file_key(), item.get_content_path(), item.get_snapshot_path(), item.get_checksum_type(), item.getPoster());
				}
				log.error(url + " - error");
			} else {
				// post complete 오류 일때 파일 삭제하는 경우 방지를 위해 complete_fail 확인
				for(FileItem item : items) {
					if(item.is_copy_complete() == true && item.complete_fail == false) {
						File f = new File(item.get_physical_path());
						if(f.exists()) {
							f.delete();
							log.debug("complete 성공한 파일 삭제 : " + item.get_physical_path());
						} else {
							log.error("complete 성공한 파일 삭제할 수 없음 : " + item.get_physical_path());
						}
					}
				}
			}
		} catch (ClientProtocolException e) {
			log.error(url + " - error");
			log.error(e.toString());
		} catch (IOException e) {
			log.error(url + " - error");
			log.error(e.toString());
		}
		
	}

	/**
	 * 작업폴더로 파일을 복사한다. <br>
	 * 서버에 파일을 먼저 복사하고 복사 성공한 데이터만 서버에 보고한다.
	 * 
	 * @param stmt
	 * @param items
	 * @return 작업폴더로 이동 시킨 파일 개수
	 */
	private int copyFileItems(Statement stmt, ArrayList<FileItem> items) {
		
		FileCopyCommander copy = new FileCopyCommander(this.conf.is_use_rsync(), this.conf.get_rsync_bwlimit());
		
		/**
		 * 서버에 이동할 파일 리스트를 전송하고 성공한 응답에 대해서 이동을 하도록 한다.
		 */	
		int count = 0;
		int _loop = 1;
		log.info("[COPY] start");
		for(FileItem item : items) {
			log.info(String.format("move : %d/%d", _loop++, items.size()));
			
			/**
			 * 물리파일이 존재하지 않는 경우 skip
			 */
			String srcPath = item.get_physical_path();
			if(item.isExistPhysicalPath() == false) {
				item.set_copy_complete(false);
				continue;
			}
			
			String destPath = String.format("%s/%s", info.getWorkDir(), item.get_content_path());
			destPath = myUtils.FilenameDefence(destPath);
			
			try {
				myUtils.makePath(destPath);
				copy.run(srcPath, destPath);

				/**
				 * 복사한 경로에 파일이 존재하는지 확인
				 */
				File dest = new File(destPath);
				
				/**
				 * 경로에 파일이 존재하지 않는다면 생성한 poster를 삭제한다.
				 */
				if(dest.exists() == false) {
					item.set_copy_complete(false);
					
					log.error("file copy error (2): " + item.get_physical_path() + " -> " + destPath);
//					item.setPosterCreated(false);
					
					String snapPath = String.format("%s/%s", conf.getSnap().getSnapDir(), item.get_snapshot_path());
					File snapFile = new File(snapPath);
					snapFile.delete();
				} else {
					item.set_copy_complete(true);
					count++;
				}
			} catch (Exception e) {					
				log.error("file copy error (1): " + item.get_physical_path() + " -> " + destPath);
//				item.setPosterCreated(false);
				
				String snapPath = String.format("%s/%s", conf.getSnap().getSnapDir(), item.get_snapshot_path());
				File snapFile = new File(snapPath);
				snapFile.delete();
			}
		}
		log.info("[COPY] end");
		return count;
	}

	/**
	 * SNAP 파일을 생성한다.<br>
	 * snapshot은 target에 직접 저장된다.<br>
	 * snapshot path가 "" 이면 snapshot snapshot은 저장하지 않는다.<br>
	 * @param items
	 */
	private void createSnapFile(ArrayList<FileItem> items) {

		for(FileItem item : items) {
			
			if(item.isExistPhysicalPath() == false) {
				continue;
			}
			
			/**
			 * snapshot path가 "" 이면 snapshot 찍은것은것으로 판단한다.
			 */
			if(myUtils.isEmpty(item.get_snapshot_path())) {
				item.set_snapshot_path("");
				continue;
			}
			
			if(item.get_media_info() == true) {
				if(item.checkSupportExt() == true) {
					/**
					 * 추가지원 파일 확장자들은 무조건 성공 반환
					 */
				} else {
					if(item.mediaInfo != null) {
						if(myUtils.isEmpty(item.mediaInfo.format) == false) {
							if(myUtils.isEmpty(item.mediaInfo.videoFormat) == false) {
								// 썸네일 확인
								SnapCreator snap = new SnapCreator(conf, "", item);
								try {
									if(snap.run(0) == false) {
										snap.run(1);
									}

									/*
									 *  스냅삿 권한 777 웹 서버에서  사용자 nginx 되어 있어서  
									 *  watcher 에서 snapshot 파일 권한을 777 셋팅함.
									 */
									String destPath = conf.getSnap().getSnapDir()+item.get_snapshot_path();
									
									// 경로 방어 코드 추가
									destPath = myUtils.FilenameDefence(destPath);
									
									/**
									 * 실제 Poster가 생성되었는지 확인하고 해당 없는 경우 path를  '' (Zero-string)으로 만든다.
									 */
									File posterFile = new File(destPath);
									if(posterFile.exists()) {
										// CMS에서 snapshot을 삭제할 수 있도록 요청받음.
										myUtils.chmod777(destPath);
									} else {
										item.set_snapshot_path("");
									}
								} catch (Exception e) {
									item.set_snapshot_path("");
									e.printStackTrace();
								}
								
							} else if(myUtils.isEmpty(item.mediaInfo.audioFormat) == false) {
								item.set_snapshot_path("");
							} else if(myUtils.isEmpty(item.mediaInfo.imageFormat) == false) {
								item.set_snapshot_path("");
							} else {
								item.set_snapshot_path("");

								/**
								 * 지원하지 않는 미디어 파일
								 * SupportExt로 걸러지지 않은 파일임 
								 */
								log.error("unsupport media file: " + item.get_physical_path());
							}
						} else {
							log.error("Utils.isEmpty(mediaInfo.format)");
							// media info를 획득하지 못해도 등록하도록 한다.
							item.set_snapshot_path("");
						}
					} else {
						log.error("mediaInfo == null");
						item.set_snapshot_path("");
					}
				}
			} else {
				// media info를 획득하지 못해도 등록하도록 한다.
				item.set_snapshot_path("");
			}
		}
	}

	/**
	 * 초기화
	 * - 썸네일 생성 임시 폴더 생성
	 * @return
	 */
	private boolean initWatcher() {
		
		/**
		 * 썸네일 생성등 임시 SNAP 폴더를 생성한다.
		 */
		String snap_temp = String.format("%s/%s", conf.getSnap().getSnapTempDir(), info.getName());
		File file_snap_temp = new File(snap_temp);
		file_snap_temp.mkdirs();

		return true;
	}

	/**
	 * 감시 대상 파일중 STATUS=0(서버에 통보하지 못한 파일)인 리스트를 생성해서 서버에 전송한다.
	 * 
	 * @param stmt
	 * @return
	 */
	private boolean sendWatchFiles(Statement stmt) {
		
		SendFileItems sendItem = new SendFileItems();		
		
		// 한번에 전송하는 최대 전송 아이템수 제한
		int nMaxCount = conf.getWatcherSendMax();
		int nCount = 0;
		ResultSet results = null;
		/**
		 * 감시 대상 리스트를 DB에서 Query 한다.
		 */
		try {
			String sqlFmt = "SELECT PATH, OLD_SIZE, NEW_SIZE, OLD_DATE, NEW_DATE, COMP_CNT, STATUS, CHK_DATE, CID, CPATH, DOMAIN, UPLOAD_PATH, SNAP_PATH, TITLE, MD5, CHECKSUM_TYPE, POSTER_POS, POSTER_WIDTH, POSTER_HEIGHT " +
//			String sqlFmt = "SELECT PATH, DOMAIN, NEW_SIZE, NEW_DATE, CHK_DATE, CID, CPATH, UPLOAD_PATH, SNAP_PATH, TITLE " +
					" FROM FILES WHERE STATUS = 0 ORDER BY PATH";
			String sql = String.format(sqlFmt);
			
			log.trace(sql);
			
			results = stmt.executeQuery(sql);
			while(results.next()) {
				
				log.trace("" +  nMaxCount + " > " + nCount + ", " + results.toString());
				/**
				 * 서버에 한번에 전송하는 감시 리스트 제한 설정
				 */
				nCount++;
				if(nMaxCount > 0 && nCount >= nMaxCount) {
					break;
				}
				
				FileItem item = FileItem.fromResultSet(results); 

				if(log.isTraceEnabled() && item.isPassThrough() == true) {
					log.trace(item.toString());
				}
				sendItem.add(item);
			}
			results.close();
		} catch (SQLException e) {
			if(results != null)
				try {
					results.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			log.error(e.toString());
			return false;
		} catch (Exception e) {
			if(results != null)
				try {
					results.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			log.error(e.toString());
			return false;
		}
		log.trace("items: " + sendItem.toString());
		/**
		 * 서버에 전송한 감지 리스트 등록에 성공하면 해당 파일의 STATUS를 1로 변경한다.
		 */
		try {
			if(sendItem.size() > 0) {
//				log.debug(sendItem.toJSONString(info.getWatcherFileKind()));

				/**
				 * 	서버에 전송한 감지 리스트 등록에 성공하면 해당 파일의 STATUS를 1로 변경한다.
				 */
				log.debug("file register 보내는값 = " + sendItem.toString());
				if(this.postWatcherFileRegister(conf.get_kollus_api().get_url(URLS.WATCHER_LIST_INSERT), sendItem) == 200) {
					log.trace("sendItem size: " +  sendItem.size());
					for(int i = 0; i < sendItem.size(); i++) {
						FileItem item = sendItem.get(i);
						log.debug("" + i + " : " + item.toString());
						/**
						 * item의 key가 없다면 해당 아이템은 서버 등록에 실패한 경우임
						 */
						if(item.get_upload_file_key() != null && item.get_upload_file_key().length() > 0) {
							log.debug("등록성공: " + item.get_upload_file_key() + ", " + item.toString());
							this.db_update_file_status(stmt, item.get_physical_path(), 1, item.get_upload_file_key(), item.get_content_path(), item.get_snapshot_path(), item.get_checksum_type(), item.getPoster());
						} else {
							log.trace("등록실패: " + item.get_upload_file_key());
							this.db_delete_item_pysicalpath(stmt, item.get_physical_path());
						}
					}
				} else {
					log.error("server return != 200 or data error");
					return false;
				}
				log.trace("postWatcherFileRegister End : " + info.getName());
			}
		} catch (IOException e) {
			log.error(e.toString());
			return false;
		}
		
		return true;
	}

	/**
	 * 작업 대상 파일의 STATUS를 변경한다.
	 * @param stmt
	 * @param chk_time
	 * @param status
	 * @return
	 */
	private boolean updateCompareOverStatus(Statement stmt, String chk_time, int status) {
		if(stmt == null) return false;
		
		String sqlFmt = "UPDATE FILES SET STATUS=%d WHERE COMP_CNT >= %d AND {FN TIMESTAMPDIFF(SQL_TSI_SECOND, NEW_DATE, {TS '%s'})} > %d ORDER BY PATH";		
		String sql = String.format(sqlFmt, status, info.getCheckinCount(), chk_time, info.getCheckinTime());
		
		try {
			stmt.executeUpdate(sql);
		} catch (SQLIntegrityConstraintViolationException e) {
			return false;
		} catch (SQLException e) {
			log.error(sql + ", " + e.toString());
			return false;
		}
		
		return true;
	}

	/**
	 * 작업 대상 파일의 리스트를 획득한다.
	 * - 비교횟수가 conf에 지정된 횟수보다 크면서 파일의 업데이트 시간이 현재 작업시간과 conf에 지정된 시간(초) 보다 크게 차이가 나야함.
	 * - 파일 확인 작업의 오류 발생시간이 NEW_DATE 보다 작은 경우 
	 * 
	 * @param stmt
	 * @param chk_time
	 * @return
	 */
	private ArrayList<FileItem> selectCompareOver(Statement stmt, String chk_time) {
		
//		ArrayList<FileItem> items = new ArrayList<FileItem>();
//		ArrayList<FileItem> errItems = new ArrayList<FileItem>();
		ArrayList<FileItem> selectItems = new ArrayList<FileItem>();
		
		try {
			/**
			 * NEW_DATE가 반드시 ERR_DATE 보다 커야한다.
			 * 오류가 발생한 이후로 사용자가 업데이트를 했어야한다.
             * 2017. 12. 21 조경원 수정
             * 업로드 완료된 파일 조회 시 정렬(Order by)순서를 의미없는
             * ERR_DATE(이값은 고정값),
             * CHK_DATE(이 값은 매번 실행할때마다 최신값임),
             * PATH인데,
             * 여기서 PATH에 서비스계정명이 들어가고 해당 명이 뒷순서일 경우
             * 먼저 등록된 파일임에도 불구하고 이름때문에 맨 나중에 검색됨.
             * 따라서 해당 조건(PATH) 삭제 하고 CID(Upload Key)순으로 변경 처리
			 * 2019.01.11 조경원 수정
			 * 작업 대상 리스트 조회 시 STATUS가 1인(등록성공인) row만 조회되도록 수정.
			 * copyComplete 처리가 오래걸릴 경우 schedule에 의해 중복 등록이 되는 case가 발생하여
			 * 해당 로직을 추가함. 추가로 copyComplete 전송 시(전송 전에) STATUS값을 2로 업데이트 함.
			 */
			String sqlFmt = "SELECT PATH, OLD_SIZE, NEW_SIZE, OLD_DATE, NEW_DATE, COMP_CNT, STATUS, CHK_DATE, CID, CPATH, DOMAIN, UPLOAD_PATH, SNAP_PATH, TITLE, MD5, CHECKSUM_TYPE, POSTER_POS, POSTER_WIDTH, POSTER_HEIGHT " +
					" FROM FILES WHERE STATUS=1 AND COMP_CNT >= %d AND TIMESTAMPDIFF(SECOND, NEW_DATE, '%s') > %d " +
					" AND CHK_DATE > ERR_DATE AND CID !='' ORDER BY ERR_DATE, CHK_DATE, CID";
			

			String sql = String.format(sqlFmt, info.getCheckinCount(), chk_time, info.getCheckinTime());
			//log.debug("selectCompareOver : " + sql);
			ResultSet results = stmt.executeQuery(sql);

			ResultSetMetaData rsmd = results.getMetaData();
//			int numberCols = rsmd.getColumnCount();
			
			int max = conf.getWatcherSendMax();
			int count = 0;
			while(results.next()) {				
				String new_date = results.getString("NEW_DATE");
				FileItem item = FileItem.fromResultSet(results);
				
				boolean bExist = item.isExistPhysicalPath();
				if(bExist == true) {
					try {
						Date d = dateFormat.parse(new_date);
						item.set_last_modified(d.getTime());
					} catch (ParseException e) {
						log.error(e.toString());
					}
					
					item.get_media_info();
					
					selectItems.add(item);
					count++;
				}
				
				log.debug(item.toString());
				
				if(count > max) {
					break;
				}
			}
			
			results.close();
		} catch (SQLException e) {
			log.error(e.toString());
			return null;
		}
		
		/**
		 * selectItems에 포함된 FileItem중 md5정보가 없는 정보에 대한 작업 수행
		 * 1. md5 checksum 구하기(md5가 on일 경우에만)
		 * 2. 로컬 정보(db) 업데이트하기
		 * 3. selectItems에 업데이트 하기
		 */
		for(FileItem item : selectItems) {
            if(item.get_checksum_type() == item.MD5) {
                // md5 정보를 구해야하는 상황이면
                if(myUtils.isEmpty(item.get_md5()) == true) {
                    item.set_md5(get_file_md5(item.get_physical_path()));
                    if(update_fileitem_md5(stmt, item) == false) {
                        log.error("md5 update error: " + item.get_physical_path());
                    }
                    log.debug(String.format("item.path:%s, item.md5:%s", item.get_physical_path(), item.get_md5()));
                }
            } else {
                log.debug(String.format("item.path:%s, item.md5 is off", item.get_physical_path()));
            }
		}
		return selectItems;
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		
	}
	
	/*
     * Calculate checksum of a File using MD5 algorithm
     */
    private String get_file_md5(String path){
        String checksum = null;
        try {
            FileInputStream fis = new FileInputStream(path);
            MessageDigest md = MessageDigest.getInstance("MD5");

			/**
			 * 2017. 09. 08 KW.CHO
			 * 파일 전체를 대상으로 hash를 계산할 경우
			 * 파일이 대용량(5G이상)이게 되면 hash계산에 너무 많은 시간이 소요되기 때문에
			 * 파일의 일부분만 읽어(Disk IO에 영향이 적은 정도) hash 계산하도록 로직 수정
			 *
			 * 2017. 12. 19 KW.CHO
			 * md5 체크하는 로직은 on/off 기능으로 대체
			 */
//			long hash_size = 1024 * 8; // default 8Kbyte
//			File file = new File(path);
//			// 파일 크기가 hash_size보다 작을 경우 파일 전체 hash 계산
//			if (file.length() < hash_size) {
//				hash_size = file.length();
//			}
            //Using MessageDigest update() method to provide input
//            byte[] buffer = new byte[(int)hash_size];
			byte[] buffer = new byte[1024*1024*10];
            int numOfBytesRead;
//            for(int totalReadBytes = 0; totalReadBytes >= hash_size || numOfBytesRead != 0; numOfBytesRead = fis.read(buffer)){
//                md.update(buffer, 0, numOfBytesRead);
//				totalReadBytes += numOfBytesRead;
//            }
			while((numOfBytesRead = fis.read(buffer)) > 0){
				md.update(buffer, 0, numOfBytesRead);
			}
            
            byte[] hash = md.digest();
            fis.close();
            
            BigInteger bigInt = new BigInteger(1,hash);
            checksum = bigInt.toString(16);
	    	// Now we need to zero pad it if you actually want the full 32 chars.
	    	while(checksum.length() < 32 ){
	    		checksum = "0"+checksum;
	    	}
        } catch (IOException ex) {
            log.error(ex.toString());
        } catch (NoSuchAlgorithmException ex) {
        	log.error(ex.toString());
        }
          
       return checksum;
    }

	/**
	 * 파일 리스트 생성 및 DB 추가 (Statment가 null이 아닌 경우 DB 추가)
	 * DB에 데이터가 존재하는 경우 Update 한다.
	 *  TODO: file update so slow... 
	 * @param stmt 
	 * @param dirs
	 * @param files
	 * @return
	 */
	private void newFileItems(Statement stmt, ArrayList<FileItem> dirs, ArrayList<FileItem> files) {
		String rootPath = info.getWatcherDir();
		
		LSParser ls = new LSParser(conf, info.getJobName(), rootPath, dirs, files);
		try {
			ls.run();
		} catch (Exception e) {
			log.error(e.toString());
		}
		
		/**
		 * update 객체수
		 */
		int update_count = 0;
		
		/**
		 * 신규 등록 객체수
		 */
		int insert_count = 0;
		
		/**
		 * 파일 존재확인 실패수
		 */
		int error_count = 0;
		
		for( FileItem f: files) {
			f.set_physical_path(f.get_physical_path().replaceAll("\\\"", "\""));
			f.set_physical_path(f.get_physical_path().replaceAll("/\"", "\""));
			File file = new File(f.get_physical_path());
			if(file.exists() == true) {
				if(isIgnoreFile(f) == false) {
					if(stmt != null) {
						if(existFileItem(stmt, f, check_time) == true) {
							update_count++;
							updateFileItem(stmt, f, check_time);
						} else {
							insert_count++;
							insertFileItem(stmt, f, check_time);
						}
					}
				}
			} else {
				error_count++;
				log.error("not exists: " + f.get_physical_path());
			}
		}
		
		/**
		 * charset이 다른 파일이 존재하는것으로 보임.
		 */
		if(insert_count == 0 && update_count == 0 && error_count > 0) {
			log.debug("convert not matched charset filename");
			
			ConvertMove convmv = new ConvertMove(conf, rootPath);
			try {
				convmv.run();
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
	}
	
	/**
	 * 파일이 설정에 포함된 무시 파일인지 확인하는 함수 
	 * 무시 파일이면 true 반환
	 * FileItem.pysical_path을 비교함 
	 * @param f
	 * @return 무시파일이면 true
	 */
	private boolean isIgnoreFile(FileItem f) {
		for(String ignoreName : conf.getIgnoreFilename()) {
			String temp = f.get_physical_path().toLowerCase();
			String ignore = ignoreName.toLowerCase();
			if(temp.startsWith(ignore) == true) {
				return true;
			}
		}
		return false;
	}

	/**
	 * FileItem에 pysical_path가 db에 존재하는지 확인하는 함수 
	 * @param stmt
	 * @param f
	 * @param check_time2 현재 시간확인하지 않음 
	 * @return
	 */
	private boolean existFileItem(Statement stmt, FileItem f, String check_time2) {
		if(stmt == null) return false;
		
		if(FileUtils.exists(f.get_physical_path()) == true) {
			String sqlFmt = "UPDATE FILES SET PATH=PATH WHERE PATH='%s'";
			String sql = String.format(sqlFmt, f.get_physical_path().replace("'", "''"));
			
			try {
				if(stmt.executeUpdate(sql) == 0) {
					return false;
				}
			} catch (SQLException e) {
				e.printStackTrace();
				log.error(sql);
				return false;
			}			
			return true;
		}
		
		return false;
	}

	/**
	 * watcher.dir의 하부에 잔여 파일이 없는 경우 폴더를 삭제한다. <br>
	 * 해당 폴더의 마지막 수정시간이 5분 이내인 경우 삭제하지 않는다. <br>
	 * 폴더의 Depth가 깊은 경우 단계적으로 삭제될 것임 <br>
	 * (하부의 폴더를 삭제하면 상위 폴더의 수정시간이 업데이트됨.)
	 */
	@Deprecated
	private void removeEmptyFolder(ArrayList<FileItem> dirs) {
		
		String rootPath = info.getWatcherDir();
		
		/**
		 * 폴더의 마지막 수정시간이 5분 이내라면 삭제하지 않는다.
		 */
		final long TERM_LIMIT = myUtils.ONE_MINUTE * 5;
		long _cur_time = System.currentTimeMillis();

		for(int i = dirs.size(); i > 0; i--) {
			FileItem f = dirs.get(i-1);
			if(rootPath.compareToIgnoreCase(f.get_physical_path()) != 0) {
				log.debug("removeEmptyFolder: " + f.get_physical_path());
				long term = _cur_time - f.get_last_modified();
				if(f.get_filesize() == 0 && term  > TERM_LIMIT) {
					File dir = new File(f.get_physical_path());
					dir.delete();
				}
			}
		}
	}

	/**
	 * 데이터베이스 연결
	 * @param jobName
	 * @return
	 * @throws Exception
	 */
	private static Connection connectDatabase(String jobName) throws Exception {		
		
		String appPath = System.getProperty("user.dir");
		String dbPath = String.format("%s/%s", appPath, jobName);
		
		Class.forName("org.h2.Driver");
		String dbURL = String.format("jdbc:h2:file:%s/MEDIAWATCHER;TRACE_LEVEL_FILE=%d;TRACE_LEVEL_SYSTEM_OUT=%d;", dbPath, H2_TRACE_LEVEL, H2_TRACE_LEVEL);
         
		Connection connect = null;
		try	{
			Class.forName("org.h2.Driver");
			connect = DriverManager.getConnection(dbURL); 
		} catch (Exception e) {
			throw e;
		}
		return connect;
	}
	
	/**
	 * 데이터베이스 연결 종료
	 * @param jobName
	 * @param stmt 
	 * @throws SQLException
	 */
	private void shutdownDerby(String jobName, Statement stmt) {
		
		if(stmt != null) {
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
	 * Database 연결 statment를 얻는다.
	 * 연결풀을 사용할때를 대비해 파라미터를 받음.
	 * 
	 * @param name
	 * @param con
	 * @return
	 * @throws SQLException
	 */
	private Statement getStatment(String name, Connection con) throws SQLException {		
		Statement stmt = con.createStatement();
		return stmt;
	}

	/**
	 * Derby 테이블을 생성한다.
	 * @param stmt
	 */
	private boolean createTable(Statement stmt) {		
		if(conn == null) return false;
		if(stmt == null) return false;
		
		/**
		 * FILES 테이블이 존재하면 그냥 패스, 없으면 생성
		 * (Apache derby) 무조건 생성하는 경우 오히려 시간이 더 걸리거나 메모리가 증가하는 현상 발견
		 * (H2)에서는 현상 발견하지 못함.
		 */
		String sql = "";
		try {
			sql = "SELECT * FROM FILES;";
			stmt.executeQuery(sql);
		} catch (SQLException e) {
			sql = "CREATE TABLE FILES (" +
					"  PATH VARCHAR(2048) PRIMARY KEY " +
					", DOMAIN VARCHAR(1024)" +
					", OLD_SIZE BIGINT" +
					", NEW_SIZE BIGINT" +
					", OLD_DATE TIMESTAMP" +	// 파일 마지막 업데이트 이전 시간
					", NEW_DATE TIMESTAMP" +	// 파일 마지막 업데이트 시간
					", ERR_DATE TIMESTAMP" +	// 미디어 확인 오류시 오류 발생 작업시간
					", CHK_DATE TIMESTAMP" +	// 최종 확인 작업 시간
					", COMP_CNT INTEGER" +		// 비교횟수
					", STATUS SMALLINT" +
					", UPLOAD_PATH VARCHAR(2048) " +	// 감시경로 (논리적 경로) {upload_path}
					", CID VARCHAR(50) " +		// 서버로 부터 획득한 CID {key}
					", CPATH VARCHAR(2048) " +	// 서버로 부터 획득한 CPATH {content_path}
					", SNAP_PATH VARCHAR(2048) " +	// SNAP 파일 경로 {snapshot_path}
					", TITLE VARCHAR(2048) " +	// TITLE {title}
					", MD5 VARCHAR(50) " +	// file md5 checksun {md5}
                    ", CHECKSUM_TYPE INT " + // checksum type{0:0ff, 1:md5}
					", POSTER_POS INT " + // poster position
					", POSTER_WIDTH INT " + // poster width
					", POSTER_HEIGHT INT " + // poster height
					")";				
			try {
				stmt.executeUpdate(sql);
			} catch (SQLException e1) {
			}
		}

		return true;
	}
	
	/**
	 * FileItem을 추가한다. 기등록된 데이터인 경우 오류 발생
	 * @param stmt
	 * @param item
	 * @return
	 * @throws SQLException
	 */
	private boolean insertFileItem(Statement stmt, FileItem item, String chk_time) {
		if(stmt == null) return false;
		
		String strDate = item.getDateString();		
		String sqlFmt = "INSERT INTO FILES(" +
                "PATH," +
                "DOMAIN," +
                "OLD_SIZE," +
                "NEW_SIZE," +
                "OLD_DATE," +
                "NEW_DATE," +
                "ERR_DATE," +
                "CHK_DATE," +
                "COMP_CNT," +
                "STATUS," +
                "UPLOAD_PATH," +
                "CID," +
                "CPATH," +
                "SNAP_PATH," +
                "TITLE," +
                "MD5," +
                "CHECKSUM_TYPE," +
                "POSTER_POS," +
                "POSTER_WIDTH," +
                "POSTER_HEIGHT) "+
                "VALUES ('%s','%s', %d, %d, '%s', '%s', '1970-01-01 01:01:01.000', '%s', 0, 0,'%s','','','','%s','%s', %d, %d, %d, %d)";
		String sql = String.format(sqlFmt, 
				item.get_physical_path().replace("'", "''"), 
				item.content_provider_key, 
				item.get_filesize(), 
				item.get_filesize(), 
				strDate, 
				strDate, 
				chk_time, 
				item.get_upload_path().replace("'", "''"),
				item.get_title().replace("'", "''"),
				"", // md5
                0, // checksum_type
				item.getPoster() == null ? -1 : item.getPoster().getPosition(),
				item.getPoster() == null ? -1 : item.getPoster().getWidth(),
				item.getPoster() == null ? -1 : item.getPoster().getHeight());
		
		try {
			stmt.executeUpdate(sql);
		} catch (SQLIntegrityConstraintViolationException e) {
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
			log.error(sql);
			return false;
		}

		return true;
	}

	/**
	 * FileItem을 갱신한다.
	 * @param stmt
	 * @param item
	 * @return
	 * @throws SQLException
	 */
	private boolean updateFileItem(Statement stmt, FileItem item, String chk_time) {
		if(stmt == null) return false;

		String sqlFmt = "UPDATE FILES SET OLD_SIZE=NEW_SIZE, NEW_SIZE=%d, OLD_DATE=NEW_DATE, NEW_DATE='%s', CHK_DATE='%s' WHERE PATH='%s'";
		String sql = String.format(sqlFmt, item.get_filesize(), item.getDateString(), chk_time, item.get_physical_path().replace("'", "''"));
		
		try {
			if(stmt.executeUpdate(sql) == 0) {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			log.error(sql);
			return false;
		}
		
		return true;
	}

	/**
	 * local database에 md5 데이터를 추가한다.
	 * @param stmt
	 * @param item
	 * @return
	 */
	private boolean update_fileitem_md5(Statement stmt, FileItem item) {

		if(stmt == null) return false;

		String sqlFmt = "UPDATE FILES SET MD5='%s' WHERE PATH='%s'";
		String sql = String.format(sqlFmt, item.get_md5(), item.get_physical_path().replace("'", "''"));
		
		try {
			if(stmt.executeUpdate(sql) == 0) {
				return false;
			}
		} catch (SQLException e) {
			log.error(e.toString());
			return false;
		}
		return true;
	}

    /**
     * local database에 checksum_type 데이터를 추가한다.
     * @param stmt
     * @param item
     * @return
     */
    private boolean update_fileitem_checksum_type(Statement stmt, FileItem item) {
        if(stmt == null) return false;

        String sqlFmt = "UPDATE FILES SET CHECKSUM_TYPE=%d WHERE PATH='%s'";
        String sql = String.format(sqlFmt, item.get_checksum_type(), item.get_physical_path().replace("'", "''"));

        try {
            if(stmt.executeUpdate(sql) == 0) {
                return false;
            }
        } catch (SQLException e) {
            log.error(e.toString());
            return false;
        }
        return true;
    }

	/**
	 * path에 해당하는 FILES 아이템의 STATUS를 변경한다.
	 * @param stmt DATABASE Statement
	 * @param path 파일의 물리적 위치
	 * @param status 업데이트할 STATUS 값
	 * @param cid  서버에서 획득한 CID값
	 * @param cpath 서버에서 획득한 CPATH 값
	 * @param poster 
	 * @return
	 */
	private boolean db_update_file_status(Statement stmt, String path, int status, String cid, String cpath, String snap_path, int checksum_type, Poster poster) {
		if(stmt == null) return false;
		
		int poster_position = poster == null ? -1 : poster.getPosition();
		int poster_width = poster == null ? -1 : poster.getWidth();
		int poster_height = poster == null ? -1 : poster.getHeight();
		
		String sqlFmt = "UPDATE FILES SET " +
				" STATUS=%d, CID='%s', CPATH='%s', SNAP_PATH='%s', " + 
				" POSTER_POS=%d, POSTER_WIDTH=%d, POSTER_HEIGHT=%d, CHECKSUM_TYPE=%d " +
				" WHERE PATH='%s'";
		String sql = String.format(sqlFmt, 
				status, cid, cpath, snap_path, 
				poster_position, poster_width, poster_height, checksum_type,
				path.replace("'", "''"));
//		log.debug(sql);
		try {
			int nUpdate = stmt.executeUpdate(sql);
//			log.debug("" + nUpdate + " : " + sql);
		} catch (SQLException e) {
			e.printStackTrace();
			log.error(sql);
			return false;
		}
		
		return true;
	}

	/**
	 * 이전 비교 데이터와 현재 데이터가 다른 경우 비교회수를 증가 시킨다.
	 * NEW_DATE가 ERR_DATE 보다 작은 경우는 증가시키지 않는다.
	 * @param stmt DATABASE Statement
	 * @return stmt가 정상이면 항상 성공한다.
	 * @throws SQLException
	 */
	private boolean updateCompareCount(Statement stmt) throws SQLException {
		if(stmt == null) return false;

		String sql = "UPDATE FILES SET COMP_CNT=COMP_CNT+1 WHERE OLD_SIZE=NEW_SIZE AND OLD_DATE=NEW_DATE AND STATUS=0 AND NEW_DATE > ERR_DATE";
		stmt.executeUpdate(sql);
		
		return true;
	}

	/**
	 * FileItem에 해당하는 아이템 삭제
	 * DB에 등록된 FileItem을 삭제한다.
	 * get_physical_path으로 삭제한다.
	 * 
	 * @param stmt DATABASE Statement
	 * @param item 삭제할 path(filename)이 저장된 FileItem
	 * @return
	 * @throws SQLException
	 */
	private boolean deleteFileItem(Statement stmt, FileItem item) {
		if(stmt == null) return false;

		return db_delete_item_pysicalpath(stmt, item.get_physical_path());
	}

	/**
	 * path로 DB에 등록된 파일 정보를 삭제한다.
	 * @param stmt
	 * @param path
	 * @return
	 */
	private boolean db_delete_item_pysicalpath(Statement stmt, String path) {
		if(stmt == null) return false;

		String sqlFmt = "DELETE FROM FILES WHERE PATH='%s'";
		String sql = String.format(sqlFmt, path.replace("'", "''"));		
		try {
			stmt.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			log.error(e.toString());
		}		
		return false;
	}

	/**
	 *  파라미터 time 시간 이전에 확인한 데이터는 모두 삭제한다.
	 *  리스트 검색에서 누락된 데이터로 판단
	 *  
	 * @param stmt DATABASE Statement
	 * @param chk_time CHK_DATE과 비교할 시간
	 * @return
	 * @throws SQLException
	 */
	private boolean deleteUnknownItem(Statement stmt, String chk_time) throws SQLException {
		if(stmt == null) return false;

		String sqlFmt = "SELECT PATH, OLD_SIZE, NEW_SIZE, OLD_DATE, NEW_DATE, COMP_CNT, STATUS, CHK_DATE, CID, CPATH, DOMAIN, UPLOAD_PATH, SNAP_PATH, TITLE, MD5, CHECKSUM_TYPE, POSTER_POS, POSTER_WIDTH, POSTER_HEIGHT " +
//		String sqlFmt = "SELECT PATH, DOMAIN, CID, CPATH, CHK_DATE, UPLOAD_PATH " +
				" FROM FILES WHERE CHK_DATE < '%s' ORDER BY PATH";
		String sql = String.format(sqlFmt, chk_time);
		
		SendFileItems sendItem = new SendFileItems();
		
		ResultSet results = stmt.executeQuery(sql);		
		while(results.next()) {
			String key = results.getString("CID");
			String content_path = results.getString("CPATH");
			/**
			 * 서버에 등록된 파일은 CID, CPATH가 존재한다.
			 * 서버에 등록된 파일은 삭제된 파일을 통보한다.
			 */
			if(myUtils.isEmpty(key) == false && myUtils.isEmpty(content_path) == false) {				
				FileItem item = FileItem.fromResultSet(results);				
				sendItem.add(item);
			}
		}
		results.close();
		
		/**
		 * 이전에 존재했으나 현재 삭제된것으로 파악된 파일리스트를 서버에 통보한다.
		 */
		if(sendItem.size() > 0) {
			if(sendDeleteFileList(sendItem) == false) {
				log.error("SEND Error: DELETE FileItem - " + sendItem.toString(info.getWatcherFileKind()));
			} else {
				/**
				 * 서버에서 정상적으로 삭제된것으로 확인이 되면 DB에서 삭제한다.
				 */
				sqlFmt = "DELETE FROM FILES WHERE CHK_DATE < '%s'";
				sql = String.format(sqlFmt, Timestamp.valueOf(chk_time));
				stmt.executeUpdate(sql);
			}
		}
				
		return true;
	}
	
	/**
	 * 삭제 파일 리스트를 서버에 전송한다.
	 * TODO : 서버에서 삭제되었다고 전송받은 파일 리스트만 삭제할것인가?
	 * @param sendItem
	 * @return
	 */
	private boolean sendDeleteFileList(SendFileItems sendItem) {
		
		// kind == 2 (http_upload)의 경우 서버에 삭제 정보를 전송하지 않는다.
		if(info.getWatcherFileKind() == 2) return true;
		
		String url = conf.get_kollus_api().get_url(URLS.WATCHER_LIST_DELETE);
		
		try {
			if(postRemoveFileItem(url, sendItem) == 200) {			
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
	 * tablename에 해당하는 모든 row를 반환한다. 디버깅용 
	 * @param stmt
	 * @param tablename
	 */
	private void selectTable(Statement stmt, String tablename, String order) {
		try {
			
			String sqlFmt = "SELECT * FROM %s %s";
			String sql = String.format(sqlFmt, tablename, myUtils.isEmpty(order) ? "" : "ORDER BY " + order);
			
			ResultSet results = stmt.executeQuery(sql);			
			ResultSetMetaData rsmd = results.getMetaData();
			int numberCols = rsmd.getColumnCount();
			
			while(results.next())
			{
				JsonObject json = new JsonObject();
				for (int i=1; i <= numberCols; i++) {
					json.addProperty(rsmd.getColumnLabel(i), results.getString(i));
				}
				log.debug(json.toString());
			}
			results.close();
		} catch (SQLException e) {
			log.error(e.toString());
		}
	}
	
    /**
     * Remove API 호출 결과 sample <br>
{ <br>
	"error": 0, <br>
	"result": { <br>
	    "watcher_files": [ <br>
	        { <br>
	            "error": 1, <br>
	            "message": "Wartcher key is empty.", <br>
	            "result": { <br>
	                "error_code": 4770, <br>
	                "error_detail": "watcher_key_is_empty", <br>
	                "content_provider_key": null, <br>
	                "key": "20140630-d1gvom39" <br>
	            } <br>
	        } <br>
	    ] <br>
	} <br>
} <br>
----
{
    "error": 0,
    "result": {
        "watcher_files": [
            {
                "error": 1,
                "message": "Web-watcher file is not exists. : IMG_1248",
                "result": {
                    "deleted_watcher_file_url": "/catenoid/IMG_1248.MOV"
                }
            },
            {
                "error": 1,
                "message": "Web-watcher file is not exists. : IMG_1251",
                "result": {
                    "deleted_watcher_file_url": "/catenoid/IMG_1251.MOV"
                }
            }
        ]
    }
}
     */
    public class ApiResult_WatcherApi {
    	@Expose
    	public int error;
    	
    	@Expose
    	public String message;
    	
    	public class WatcherFile {
    		@Expose
    		public int error;
    		@Expose
    		public String message;
    		
    		class Result {
    			@Expose
    			public int error_code;
    			
    			@Expose
    			public String error_detail;
    			
    			@Expose
    			public String content_provider_key;
    			
    			@Expose
    			public String key;
    			
    			@Expose
    			public int media_content_id;
    			
    			@Expose
    			public String content_path;
    			
    			@Expose
    			public String upload_path;
    			
    			@Expose
    			public int is_audio_file;

                @Expose
                public int checksum_type;
    			
    			@Expose
    			public String snapshot_path;
    			
    			@Expose
    			public String physical_path;
    			
    			@Expose
    			public String deleted_watcher_file_upload_url;
    			
    			@Expose
    			public Poster poster;
    		}
    		@Expose
    		public Result result;
    	}

    	class Result {
    		@Expose
			public int error_code;
    		
    		@Expose
			public String error_detail;
    		
    		@Expose
        	public WatcherFile[] watcher_files;        		
    	}
    	@Expose
    	public Result result;    	        	
    }
	
	/**
	 * 감시하던 파일이 리스트에서 제거되면 CMS에 통보한다.
	 * @param url
	 * @param sendItem
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ClientProtocolException, IOException
	 */
	private int postRemoveFileItem(String url, SendFileItems sendItem) throws ClientProtocolException, IOException {

		boolean isConnected = false;
		HttpPost httpPost = null;
		HttpResponse response = null;

		// 2017. 08. 22 재시도 로직 추가(KWCHO)
		for (int i = 0; i < 3; i++) {
			//HttpClient httpClient = agent.newHttpClient();
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

			if(isConnected) break;
			else log.debug(String.format("postRemoveFileItem CMS API(%s) connection retry(%d)", url, i + 1));

			try { Thread.sleep(1000); } catch (Exception e) {}
		}

		if(!isConnected || response == null) {
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
			int contentLength = (int) response.getEntity().getContentLength();

			responseBody = HttpAgent.generateString(inputStream, charset);
			//inputStream.close();

			if (nStatus == 200) {
				log.trace(responseBody);
				
				try {
					Gson gson = BaseCommand.gson(false);
					ApiResult_WatcherApi api_result = gson.fromJson(responseBody, ApiResult_WatcherApi.class);
					
					if (api_result.error != 0) {						
						log.error(api_result.message);

						if (api_result.result != null && api_result.result.error_code > 0) {
							this.sendErrorReport(api_result.result.error_code, api_result.message, "");
						}
					} else {
						for(WatcherFile item : api_result.result.watcher_files) {
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
			if (httpPost != null) httpPost.abort();
		}
		return nStatus;
	}
	
	/**
	 * watcher.list.complete 경로에 complete 정보를 전송한다.
	 * @param stmt
	 * @param url
	 * @param sendItem
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private int postWatcherFileComplete(Statement stmt, String url, SendFileItems sendItem) throws ClientProtocolException, IOException {

		boolean isConnected = false;
		HttpPost httpPost = null;
		HttpResponse response = null;

		// 2017. 08. 22 재시도 로직 추가(KWCHO)
		for(int i = 0; i < 3; i++) {
			//HttpClient httpClient = agent.newHttpClient();
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3 * 1000).build();
			HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

			List<NameValuePair> qparams = new ArrayList<NameValuePair>();
			qparams.add(new BasicNameValuePair(PARAM.API_KEY, info.getApiKey()));
			qparams.add(new BasicNameValuePair(PARAM.API_REFERENCE, info.getApiReference()));

			String jsonStr = sendItem.toString(info.getWatcherFileKind());
			qparams.add(new BasicNameValuePair(PARAM.WATCHER_FILES, jsonStr));
			log.debug(jsonStr);

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

			if(isConnected) break;
			else log.debug(String.format("postWatcherFileComplete CMS API(%s) connection retry(%d)", url, i + 1));

			try { Thread.sleep(1000); } catch (Exception e) {}
		}

		if(!isConnected || response == null) {
			log.error(LogAction.HTTP_LOG + url + ", status: connection failed");
			this.sendErrorReport(503, URLS.WATCHER_LIST_DELETE, "");
			return 503;
		}

		String responseBody = "";
		InputStream inputStream = null;
		int nStatus = response.getStatusLine().getStatusCode();
        try{

			final String charset = HttpAgent.getContentCharSet(DEFAULT_CHARSET, response.getEntity());
			inputStream = response.getEntity().getContent();
			int contentLength = (int) response.getEntity().getContentLength();

			responseBody = HttpAgent.generateString(inputStream, charset);

	        if(nStatus == 200) {
	            try {
					Gson gson = BaseCommand.gson(false);
					ApiResult_WatcherApi api_result = gson.fromJson(responseBody, ApiResult_WatcherApi.class);
					if (api_result.error != 0) {
						log.error(responseBody);
						if (api_result.result != null && api_result.result.error_code > 0) {
							this.sendErrorReport(api_result.result.error_code, api_result.message, "");
						}
					} else {
						for(WatcherFile item : api_result.result.watcher_files) {
							if (item.error != 0) {
								log.error("error code: " + item.error + ", error message: " + item.message);
								if(item.result != null) {
									FileItem findItem = findSendItem(sendItem, item.result.key);
									if(findItem != null) {
										// working file 삭제 및 snapshot 삭제
										this.transcodeCancel(findItem);
										this.deleteFileItem(stmt, findItem);
										findItem.complete_fail = true;
									} else {
										log.error("실패한 파일 정보를 찾을 수 없습니다. [" + item.result.key + "]");
									}
									/**
									 * error_code가 반환되면 추가 처리함.
									 */
									if (item.result.error_code > 0) {
										this.sendErrorReport(item.result.error_code, item.result.error_detail, "");
									}
								} else {
								}
							} else {	// success 이후 파일디비에 watch file item 삭제
								
								// error가 아닌 경우만 media_content_id가 있으나 사용처가 없어 삭제함
								FileItem findItem = findSendItem(sendItem, item.result.key);
								if(findItem != null) {
									this.deleteFileItem(stmt, findItem);
								} else {
									log.error("FileItem  파일 정보를 찾을 수 없습니다. [" + item.result.key + "]");
								}
							}								
						}
					}
	            } catch (Exception e) {
	            	log.error(e.toString());
	            	log.error(responseBody);
	            	this.sendErrorReport(5002, "JSON Error: " + URLS.WATCHER_LIST_COMPLETE, "");
	            }
			} else {
				this.sendErrorReport(nStatus, URLS.WATCHER_LIST_COMPLETE, "");
				log.error(url + "(" + nStatus + ") : " + responseBody);
			}
       }finally{
        	if(inputStream !=null) inputStream.close();
        	if(httpPost != null) httpPost.abort();
        }
        return nStatus;
	}
	
	/**
	 * sendItem에 있는 FileImte에 동일한 key가 있으면 해당 FileItem을 반환한다.<br>
	 * 찾지못하면 null을 반환한다.
	 * @param sendItem
	 * @param key
	 * @return FileItem or null
	 */
	private FileItem findSendItem(SendFileItems sendItem, String key) {
		for(FileItem item : sendItem) {
			if(item.get_upload_file_key().compareTo(key) == 0) {
				return item;
			}
		}
		return null;
	}

	/**
	 * 서버에 감시대상 파일 리스트를 등록한다.
	 * 서버는 각각의 파일마다 등록 여부를 Result에 담아준다.
	 * 성공한 파일만 상태를 바꾸어 주어야한다.
	 *
	 * @param url
	 * @param sendItem
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ClientProtocolException, IOException
	 */
	private int postWatcherFileRegister(String url, SendFileItems sendItem) throws ClientProtocolException, IOException {

		HttpPost httpPost = null;
		HttpResponse response = null;

		for(int i = 0; i < 3; i++) {
			//HttpClient httpClient = agent.newHttpClient();
			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3 * 1000).build();
			HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

			List<NameValuePair> qparams = new ArrayList<NameValuePair>();
			qparams.add(new BasicNameValuePair(PARAM.API_KEY, info.getApiKey()));
			qparams.add(new BasicNameValuePair(PARAM.API_REFERENCE, info.getApiReference()));

			String watcherFiles = sendItem.toString(info.getName(), info.getWatcherFileKind());
			qparams.add(new BasicNameValuePair(PARAM.WATCHER_FILES, watcherFiles));

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(qparams, DEFAULT_CHARSET);

			httpPost = new HttpPost(url);
			httpPost.setEntity(entity);

			try {
				response = httpClient.execute(httpPost);
                break;
			} catch (IOException e) {
				log.error(Utils.getStackTrace(e));
			} catch (Exception e) {
				log.error(Utils.getStackTrace(e));
			}

            // 재시도 로직
			log.debug(String.format("postWatcherFileRegister CMS API(%s) connection retry(%d)", url, i + 1));
			try { Thread.sleep(1000); } catch (Exception e) {}
		}

		String responseBody = "";
		InputStream inputStream = null;
        int nStatus = response.getStatusLine().getStatusCode();
        try{
        	if(nStatus == 200) {
        		final String charset = HttpAgent.getContentCharSet(DEFAULT_CHARSET, response.getEntity());
        		inputStream = response.getEntity().getContent();
        		int contentLength = (int) response.getEntity().getContentLength();
				responseBody = HttpAgent.generateString(inputStream, charset);
				inputStream.close();
				try {
					Gson gson = BaseCommand.gson(false);
					ApiResult_WatcherApi api_result = gson.fromJson(responseBody, ApiResult_WatcherApi.class);
					// Api Error
					if(api_result.error != 0) {
						log.error(responseBody);
						if (api_result.result != null && api_result.result.error_code > 0) {
							this.sendErrorReport(api_result.result.error_code, api_result.message, "");
						}
					} else {
						for(WatcherFile item : api_result.result.watcher_files) {
							// Item Error
							if (item.error != 0) {
								log.error(item.message);
								if(item.result != null) {
									if(item.result.deleted_watcher_file_upload_url != null) {
										String deleted_watcher_file_url = item.result.deleted_watcher_file_upload_url;
										log.info("서버 요청에 의해 Watcher파일 삭제: " + deleted_watcher_file_url);									
										if(deleted_watcher_file_url.indexOf(info.getWatcherDir()) == 0) {
											String watcher_filepath = String.format("%s", deleted_watcher_file_url);
											File watcherFile = new File(watcher_filepath);
											watcherFile.delete();
											if(watcherFile.exists() == true) {
												log.error("요청에 의해 삭제하려고 했으나 삭제 실패함: " + watcher_filepath);
											} else {
												log.info("요청에 의해 삭제: " + watcher_filepath);
											}
										} else {
											log.error("삭제 경로 시작이 watcherDir이 아님 : " + deleted_watcher_file_url);
										}
									}
									/**
									 * error_code가 반환되면 추가 처리함.
									 */
									if (item.result.error_code > 0) {
										this.sendErrorReport(item.result.error_code, item.result.error_detail, "");
									}
								}
							} else {
								/**
								 * error == 0인 등록에 성공한 파일
								 */
								FileItem f = FileItem.fromApiResult(item);
								sendItem.update(f);
							}
						}
					}
				} catch (Exception e) {
					log.error(myUtils.getStackTrace(e));
					log.error(responseBody);
	            	this.sendErrorReport(5002, "JSON Error: " + URLS.WATCHER_LIST_INSERT, "");
				}
	        } else {
	        	this.sendErrorReport(nStatus, URLS.WATCHER_LIST_INSERT, "");
	        	log.error("Http != 200, " + url );
	        }
        }finally{
            if(inputStream !=null) inputStream.close();
            if(httpPost != null) httpPost.abort();
         }     
        return nStatus;
	}

	/**
	 * 에러정보를 서버로 전송한다.
	 * @param i_err
	 * @param n_msg
	 * @param content_provider_key
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private void sendErrorReport(int i_err, String n_msg, String content_provider_key) {
		
		String url = conf.get_kollus_api().get_url(URLS.MODULE_ERROR_REPORT);		
		//HttpClient httpClient = agent.newHttpClient();
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
		
		if(builder != null) {
	        URI uri = null;
			try {
				uri = builder.build();
			} catch (URISyntaxException e) {
				log.error(e.toString());
			}
			
			if(uri != null) {
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
	
}
