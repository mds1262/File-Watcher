package net.catenoid.watcher.job;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.upload.FtpUploadService;
import net.catenoid.watcher.upload.KusUploadService;
import net.catenoid.watcher.upload.utils.CommonUtils;
import net.catenoid.watcher.uploadImp.FtpUploadServiceImp;
import net.catenoid.watcher.uploadImp.KusUploadServiceImp;

@DisallowConcurrentExecution
public class Role_Watcher implements InterruptableJob {
//	private static final int THREAD_POOL = 10;
//	private static final int H2_TRACE_LEVEL = 0;
	private static Logger log = Logger.getLogger(Role_Watcher.class);
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	static {
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	// private static String NEW_LINE = System.getProperty("line.separator");

	/**
	 * HTTP 통신에 사용될 기본 문자열 CHARSET 설정 (UTF-8)
	 */
//	private static String DEFAULT_CHARSET = "UTF-8";

	/**
	 * derby 데이터베이스 파일 연결 정보
	 */
//	private Connection conn = null;
	private WatcherFolder info = null;
	private Config conf = null;

	/**
	 * Job을 시작한 시간 (dateFormat style에 따른다.)
	 */
//	private String check_time = "";

	/**
	 * Http 통신용 공용 Agent
	 */
//	private HttpAgent agent = new HttpAgent();

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
		 * 환경설정 정보를 얻는다. 정보가 잘못된 경우 Exception 발생
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
		if (ctx != null) {
			info = (WatcherFolder) ctx.getMergedJobDataMap().get("data");
		}

		log.trace("execute : " + info.getName());

		/**
		 * 작업의 시작을 서버에 통보하도록 업데이트 한다.
		 */
//		long _chk_time = System.currentTimeMillis();
		// 작업 완료후에 작업시간을 업데이트 함
		// JobStatus.getJobStatus().updateWorkTime(info.getName(), _chk_time, 0);

		/**
		 * Job에 필요한 초기화 작업 실행
		 */
		if (initWatcher() == false) {
			log.error("initWatcher Error: " + info.getName());
			return;
		}

		/**
		 * Job을 실행한 시작 시간을 문자열로 보관 동일한 작업시점에 같은 시간을 사용하기 위해서 사용함
		 */
//		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")); 

//		Date now = new Date();
//		check_time = dateFormat.format(now);

		// KUS UPLOAD => dir = HTTP_UPLOAD
		// KUS HTTP UPLOAD 폴더에 다운시 사
		// TODO 조건 처리해야됨
		if (info.getWatcherFileKind() != 1) {
			try {
				// httpUpload Module 인스턴스 생성
				KusUploadService service = new KusUploadServiceImp(info, conf);
				// 업로드된 파일읽어서 찾기
				service.findWorkFileList();
				
				service.moveToWorkFiles();
				
				int cnt = service.sendCompleteWorkFiles();
				
				if(cnt > 0) {
					log.info("Finish Transcoding Work transfer files count : " + cnt);
				}

				// 완료된파일을 API 발송
//				if(isMoveFiles) {
//				hui.sendCompleteFiles(fileList, conf);	
//				}

				// 장시간 사용안하는 파일 제거
//				hui.removeTimeOutFiles();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getStackTrace() + " : " + e.getMessage().toString());
			}
		} else {
			FtpUploadService service = new FtpUploadServiceImp(info, conf);
			try {
				service.renewWorkFileList();

				service.findWorkFileList();
			} catch (Exception e) {
				// TODO: handle exception
				CommonUtils utils = new CommonUtils();
				utils.ExceptionLogPrint(e);
			}
		}
	}

	/**
	 * 초기화 - 썸네일 생성 임시 폴더 생성
	 * 
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

	@Override
	public void interrupt() throws UnableToInterruptJobException {

	}

}
