package net.catenoid.watcher.uploadImp;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import com.kollus.json_data.config.ModuleConfig.URLS;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.files.ConvertMove;
import net.catenoid.watcher.upload.FtpUploadService;
import net.catenoid.watcher.upload.config.H2DB;
import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.upload.dto.SendFileItemsDTO;
import net.catenoid.watcher.upload.utils.FtpUploadUtils;
import net.catenoid.watcher.upload.utils.LSParser;
import net.catenoid.watcher.uploadDao.FtpUploadDao;

public class FtpUploadServiceImp extends FtpUploadDao implements FtpUploadService {
	private static Logger log = Logger.getLogger(FtpUploadServiceImp.class);
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private ArrayList<FileItemDTO> lsFiles = null;
	private ArrayList<FileItemDTO> lsDirs = null;

	/**
	 * Job을 시작한 시간 (dateFormat style에 따른다.)
	 */
	private String check_time;
	/**
	 * Role_Watcher에서 예외 발생시 종료시키기 위해사용
	 */

	public FtpUploadServiceImp() {
	}

	/**
	 * FTPUpload시 아래의 생성자실 기본 Config 및 Watcher 정보 전달 DB Connection 및 Statement 생성
	 * 
	 * @param info WatcherFolder
	 * @param conf Config
	 * @throws Exception
	 */

	public FtpUploadServiceImp(WatcherFolder info, Config conf) {
		super.utils = new FtpUploadUtils(info, conf);

		Date now = new Date();
		this.check_time = dateFormat.format(now);

		try {
			super.conn = H2DB.connectDatabase(info);
			super.stmt = utils.getStatment(conn);
			
			createTable();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("FtpUploadServiceImp Construct Err : " + e.getMessage());
			utils.shutdownDerby(conn, stmt);
		}
	}
	
	/**
	 * 신규로 들어온 파일 및 기존 파일 정리
	 */
	@Override
	public void renewWorkFileList() throws Exception  {
		// TODO Auto-generated method stub
		this.lsFiles = new ArrayList<FileItemDTO>();
		this.lsDirs = new ArrayList<FileItemDTO>();

		if (stmt == null) {
			return;
		}
	
		try {
			newFileItems(this.lsFiles, this.lsDirs);

			deleteUnknownItem(check_time);

			updateCompareCount();

		} catch (Exception e) {
			// TODO: handle exception
			utils.ExceptionLogPrint(e);
			stmt.close();
        	utils.sendErrorReport(5002, "JSON Error: " + URLS.WATCHER_LIST_COMPLETE, "");
		}
	}	

	/**
	 * 작업할 파일들은 찾기
	 */
	@Override
	public void findWorkFileList() throws Exception {
		// TODO Auto-generated method stub
		try {
			int sendRegistCnt = sendWatchFiles();
			if(sendRegistCnt > 0) {
				log.info("Send to register count : " + sendRegistCnt);
			}	

			/**
			 * 작업 대상 파일 리스트를 획득한다.
			 */
			ArrayList<FileItemDTO> items = selectCompareOver(check_time);
			if (items.size() > 0 && items != null) {
				log.info("Working file count : " + items.size());
				
				utils.createSnapFile(items);
			}
			
			if (utils.moveToWorkDir(items) > 0) {
				postCopyCompleteFiles(items);
			}
//			else {
//				log.warn("selectCompareOver == item-size: " + items.size() + ", " + utils.info.getName());
//			}
			
			removeNotExistFile();

		} catch (Exception e) {
			// TODO: handle exception
			utils.ExceptionLogPrint(e);
        	utils.sendErrorReport(5002, "JSON Error: " + URLS.WATCHER_LIST_COMPLETE, "");
		}finally {
			stmt.close();
		}
	}

	/**
	 * Derby 테이블을 생성한다.
	 * 
	 * @throws SQLException
	 */

	private void createTable() throws SQLException {
		if (conn == null) {
			log.warn("Not connect H2 database");
			return;
		}
		if (stmt == null) {
			log.warn("Not Statement instance");
			return;
		}

		/**
		 * FILES 테이블이 존재하면 그냥 패스, 없으면 생성 (Apache derby) 무조건 생성하는 경우 오히려 시간이 더 걸리거나 메모리가
		 * 증가하는 현상 발견 (H2)에서는 현상 발견하지 못함.
		 */
		String sql = "SHOW TABLES";

//		sql = "SELECT * FROM FILES;";

		ResultSet rs = stmt.executeQuery(sql);

		int num = 0;

		while (rs.next()) {
			num++;
		}

		if (num == 0) {
			createNewTable();
		}
	}

	/**
	 * 파일 리스트 생성 및 DB 추가 (Statment가 null이 아닌 경우 DB 추가) DB에 데이터가 존재하는 경우 Update 한다.
	 * TODO: file update so slow...
	 * 
	 * @param stmt
	 * @param dirs
	 * @param files
	 * @return
	 * @return
	 */

	private void newFileItems(ArrayList<FileItemDTO> dirs, ArrayList<FileItemDTO> files) throws Exception {
		WatcherFolder info = utils.getInfo();
		Config conf = utils.getConf();

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

		for (FileItemDTO f : files) {
			f.setPhysicalPath(f.getPhysicalPath().replaceAll("\\\"", "\""));
			f.setPhysicalPath(f.getPhysicalPath().replaceAll("/\"", "\""));
			File file = new File(f.getPhysicalPath());
			if (!file.exists()) {
				error_count++;
				log.error("not exists: " + f.getPhysicalPath());
				continue;
			}

			if (utils.isIgnoreFile(f) == false) {
				if (existFileItem(f, check_time) == true) {
					update_count++;
					updateFileItem(f, check_time);
				} else {
					insert_count++;
					insertFileItem(f, check_time);
				}
			}
		}

		/**
		 * charset이 다른 파일이 존재하는것으로 보임.
		 */
		if (insert_count == 0 && update_count == 0 && error_count > 0) {
			log.debug("convert not matched charset filename");

			ConvertMove convmv = new ConvertMove(conf, rootPath);

			convmv.run();
		}
	}

	/**
	 * 작업폴더로 이동시킨 파일 정보를 서버로 전송한다.<br>
	 * post에 성공하면 파일을 삭제한다.
	 * 
	 * @param stmt
	 * @param items
	 * @return
	 */
	private void postCopyCompleteFiles(ArrayList<FileItemDTO> items) throws Exception {
		String url = utils.conf.get_kollus_api().get_url(URLS.WATCHER_LIST_COMPLETE);
		SendFileItemsDTO sendItem = new SendFileItemsDTO();

		for (FileItemDTO item : items) {
			if (item.isCopyComplete() == true) {
				// CopyComplete 전송 대상 파일들의 STATUS값을 2로 설정한다.
				db_update_file_status(item.getPhysicalPath(), 2, item.getUploadFileKey(), item.getContentPath(),
				item.getSnapshotPath(), item.getChecksumType(), item.getPoster());
				sendItem.add(item);
				log.trace(item.toString());
			} else {
				log.debug("failed copy file item: " + item.toString());
			}
		}

		if (sendFtpCompleteApiCnt(sendItem) == 0) {
			for (FileItemDTO item : sendItem) {
				// 오류일 경우 재 전송을 위해 STATUS값을 1로 rollback 시킨다.
				this.db_update_file_status(item.getPhysicalPath(), 1, item.getUploadFileKey(),
						item.getContentPath(), item.getSnapshotPath(), item.getChecksumType(), item.getPoster());
			}
			log.error(url + " - error");
		} else {
			// post complete 오류 일때 파일 삭제하는 경우 방지를 위해 complete_fail 확인
			int successCnt = 0; 
			for (FileItemDTO item : items) {
				if (item.isCopyComplete() == true && !item.isCompleteFail()) {
					File f = new File(item.getPhysicalPath());
					if (f.exists()) {
						f.delete();
						successCnt += 1;
						log.info("complete 성공한 파일 삭제 (" + successCnt + ") : " + item.getPhysicalPath());
					} else {
						successCnt += 1;
						log.info("complete 성공 갯수(" + successCnt+ ") : " + item.getPhysicalPath());
					}
				}
			}
		}
	}
}
