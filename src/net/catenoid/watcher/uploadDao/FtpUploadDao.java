package net.catenoid.watcher.uploadDao;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.h2.store.fs.FileUtils;

import net.catenoid.watcher.upload.dto.KollusApiWatchersDTO;
import net.catenoid.watcher.upload.dto.KollusApiWatcherContentDTO;
import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.upload.dto.SendFileItemsDTO;
import net.catenoid.watcher.upload.utils.CommonUtils;
import net.catenoid.watcher.upload.utils.FtpUploadUtils;
import net.catenoid.watcher.upload.utils.Poster;
import net.catenoid.watcher.utils.WatcherUtils;

public class FtpUploadDao {
	private static Logger log = Logger.getLogger(FtpUploadDao.class);
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	protected Statement stmt = null;
	protected Connection conn = null;
	protected FtpUploadUtils utils = null;

	public FtpUploadDao() {
	}

//	public FtpUploadDao(Connection conn, Statement stmt){
//		this.conn = conn;
//		this.stmt = stmt;
//	}

	/**
	 * 기존의 Table이 없을경우 신규로 생성
	 * 
	 * @throws SQLException
	 */
	protected void createNewTable() throws SQLException {
		String sql = "CREATE TABLE FILES (" + "  PATH VARCHAR(2048) PRIMARY KEY " + ", DOMAIN VARCHAR(1024)"
				+ ", OLD_SIZE BIGINT" + ", NEW_SIZE BIGINT" + ", OLD_DATE TIMESTAMP" + // 파일 마지막 업데이트 이전 시간
				", NEW_DATE TIMESTAMP" + // 파일 마지막 업데이트 시간
				", ERR_DATE TIMESTAMP" + // 미디어 확인 오류시 오류 발생 작업시간
				", CHK_DATE TIMESTAMP" + // 최종 확인 작업 시간
				", COMP_CNT INTEGER" + // 비교횟수
				", STATUS SMALLINT" + ", UPLOAD_PATH VARCHAR(2048) " + // 감시경로 (논리적 경로) {upload_path}
				", CID VARCHAR(50) " + // 서버로 부터 획득한 CID {key}
				", CPATH VARCHAR(2048) " + // 서버로 부터 획득한 CPATH {content_path}
				", SNAP_PATH VARCHAR(2048) " + // SNAP 파일 경로 {snapshot_path}
				", TITLE VARCHAR(2048) " + // TITLE {title}
				", MD5 VARCHAR(50) " + // file md5 checksun {md5}
				", CHECKSUM_TYPE INT " + // checksum type{0:0ff, 1:md5}
				", POSTER_POS INT " + // poster position
				", POSTER_WIDTH INT " + // poster width
				", POSTER_HEIGHT INT " + // poster height
				")";

		stmt.executeUpdate(sql);
	}

	/**
	 * FileItem에 pysical_path가 db에 존재하는지 확인하는 함수
	 * 
	 * @param stmt
	 * @param f
	 * @param check_time2 현재 시간확인하지 않음
	 * @return
	 */
	protected boolean existFileItem(FileItemDTO f, String check_time2) throws SQLException {
		if (stmt == null)
			return false;

		if (!FileUtils.exists(f.getPhysicalPath())) {
			return false;
		}

		String sqlFmt = "UPDATE FILES SET PATH=PATH WHERE PATH='%s'";
		String sql = String.format(sqlFmt, f.getPhysicalPath().replace("'", "''"));

		if (stmt.executeUpdate(sql) == 0) {
			return false;
		}

		return true;
	}

	/**
	 * FileItem을 갱신한다.
	 * 
	 * @param stmt
	 * @param item
	 * @return
	 * @throws SQLException
	 */
	protected boolean updateFileItem(FileItemDTO item, String chk_time) {
		if (stmt == null)
			return false;

		String sqlFmt = "UPDATE FILES SET " + "OLD_SIZE=NEW_SIZE, " + "NEW_SIZE=%d, " + "OLD_DATE=NEW_DATE, "
				+ "NEW_DATE='%s', " + "CHK_DATE='%s'" + " WHERE PATH='%s'";
		String sql = String.format(sqlFmt, item.getFilesize(), item.getDateString(), chk_time,
				item.getPhysicalPath().replace("'", "''"));

		try {
			if (stmt.executeUpdate(sql) == 0) {
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
	 * FileItem을 추가한다. 기등록된 데이터인 경우 오류 발생
	 * 
	 * @param stmt
	 * @param item
	 * @return
	 * @throws SQLException
	 */
	protected boolean insertFileItem(FileItemDTO item, String chk_time) {
		if (stmt == null)
			return false;

		String strDate = item.getDateString();
		String sqlFmt = "INSERT INTO FILES(" + "PATH," + "DOMAIN," + "OLD_SIZE," + "NEW_SIZE," + "OLD_DATE,"
				+ "NEW_DATE," + "ERR_DATE," + "CHK_DATE," + "COMP_CNT," + "STATUS," + "UPLOAD_PATH," + "CID," + "CPATH,"
				+ "SNAP_PATH," + "TITLE," + "MD5," + "CHECKSUM_TYPE," + "POSTER_POS," + "POSTER_WIDTH,"
				+ "POSTER_HEIGHT) "
				+ "VALUES ('%s','%s', %d, %d, '%s', '%s', '1970-01-01 01:01:01.000', '%s', 0, 0,'%s','','','','%s','%s', %d, %d, %d, %d)";
		String sql = String.format(sqlFmt, item.getPhysicalPath().replace("'", "''"), item.getContentProviderKey(),
				item.getFilesize(), item.getFilesize(), strDate, strDate, chk_time,
				item.getUploadPath().replace("'", "''"), item.getTitle().replace("'", "''"), "", // md5
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
	 * 파라미터 time 시간 이전에 확인한 데이터는 모두 삭제한다. 리스트 검색에서 누락된 데이터로 판단
	 * 
	 * @param stmt     DATABASE Statement
	 * @param chk_time CHK_DATE과 비교할 시간
	 * @return
	 * @throws SQLException
	 */
	protected void deleteUnknownItem(String chk_time) throws Exception {
		String sqlFmt = "SELECT PATH, OLD_SIZE, NEW_SIZE, OLD_DATE, NEW_DATE, COMP_CNT, STATUS, CHK_DATE, CID, CPATH, DOMAIN, UPLOAD_PATH, SNAP_PATH, TITLE, MD5, CHECKSUM_TYPE, POSTER_POS, POSTER_WIDTH, POSTER_HEIGHT "
				+ " FROM FILES WHERE CHK_DATE < '%s' ORDER BY PATH";
		SendFileItemsDTO sendItem = new SendFileItemsDTO();
		ResultSet results = null;

		try {
			String sql = String.format(sqlFmt, chk_time);

			results = stmt.executeQuery(sql);
			while (results.next()) {
				String key = results.getString("CID");
				String content_path = results.getString("CPATH");
				/**
				 * 서버에 등록된 파일은 CID, CPATH가 존재한다. 서버에 등록된 파일은 삭제된 파일을 통보한다.
				 */
				if (WatcherUtils.isEmpty(key) == false && WatcherUtils.isEmpty(content_path) == false) {
					FileItemDTO item = CommonUtils.fromResultSet(results, "", "", 0, 0, "");
					sendItem.add(item);
				}
			}

			/**
			 * 이전에 존재했으나 현재 삭제된것으로 파악된 파일리스트를 서버에 통보한다.
			 */
			if (sendItem.size() > 0) {
				if (utils.sendDeleteFileList(sendItem) == false) {
					log.error("SEND Error: DELETE FileItem - " + sendItem.toString(utils.info.getWatcherFileKind()));
				} else {
					/**
					 * 서버에서 정상적으로 삭제된것으로 확인이 되면 DB에서 삭제한다.
					 */
					sqlFmt = "DELETE FROM FILES WHERE CHK_DATE < '%s'";
					sql = String.format(sqlFmt, Timestamp.valueOf(chk_time));
					stmt.executeUpdate(sql);
				}
			}
		} finally {
			results.close();
		}
	}

	/**
	 * 이전 비교 데이터와 현재 데이터가 다른 경우 비교회수를 증가 시킨다. NEW_DATE가 ERR_DATE 보다 작은 경우는 증가시키지
	 * 않는다.
	 * 
	 * @param stmt DATABASE Statement
	 * @return stmt가 정상이면 항상 성공한다.
	 * @throws SQLException
	 */
	protected void updateCompareCount() throws SQLException {
		String sql = "UPDATE FILES SET COMP_CNT=COMP_CNT+1 WHERE OLD_SIZE=NEW_SIZE AND OLD_DATE=NEW_DATE AND STATUS=0 AND NEW_DATE > ERR_DATE";
		stmt.executeUpdate(sql);
	}

	/**
	 * 감시 대상 파일중 STATUS=0(서버에 통보하지 못한 파일)인 리스트를 생성해서 서버에 전송한다.
	 * 
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	protected int sendWatchFiles() throws Exception {

		SendFileItemsDTO sendItem = new SendFileItemsDTO();

		// 한번에 전송하는 최대 전송 아이템수 제한
		int nMaxCount = utils.conf.getWatcherSendMax();
		int nCount = 0;
		ResultSet results = null;
		/**
		 * 감시 대상 리스트를 DB에서 Query 한다.
		 */
		try {
			String sqlFmt = "SELECT PATH, OLD_SIZE, NEW_SIZE, OLD_DATE, NEW_DATE, COMP_CNT, STATUS, CHK_DATE, CID, CPATH, DOMAIN, UPLOAD_PATH, SNAP_PATH, TITLE, MD5, CHECKSUM_TYPE, POSTER_POS, POSTER_WIDTH, POSTER_HEIGHT "
					+ " FROM FILES WHERE STATUS = 0 ORDER BY PATH";
			String sql = String.format(sqlFmt);

//			log.debug(sql);
			results = stmt.executeQuery(sql);
			while (results.next()) {
				log.debug("sql result = " + results.toString());
				log.trace("" + nMaxCount + " > " + nCount + ", " + results.toString());
				/**
				 * 서버에 한번에 전송하는 감시 리스트 제한 설정
				 */
				nCount++;
				if (nMaxCount > 0 && nCount >= nMaxCount) {
					break;
				}

				FileItemDTO item = CommonUtils.fromResultSet(results, "", "", 0, 0, "");

				log.debug(item.toString());

				if (log.isTraceEnabled()) {
					log.trace(item.toString());
				}

				sendItem.add(item);
			}

			log.trace("items: " + sendItem.toString());

			/**
			 * 서버에 전송한 감지 리스트 등록에 성공하면 해당 파일의 STATUS를 1로 변경한다.
			 */
			if (sendItem.size() == 0) {
				return 0;
			}
			/**
			 * 서버에 전송한 감지 리스트 등록에 성공하면 해당 파일의 STATUS를 1로 변경한다.
			 */
			log.debug("file register 보내는값 = " + sendItem.toString());
			boolean isUploadOrDelete = false;

			boolean isSend = utils.sendFtpRegisterApi(sendItem);
			if (isSend) {
				log.trace("sendItem size: " + sendItem.size());
				for (int i = 0; i < sendItem.size(); i++) {
					FileItemDTO item = sendItem.get(i);
					log.debug("" + i + " : " + item.toString());
					/**
					 * item의 key가 없다면 해당 아이템은 서버 등록에 실패한 경우임
					 */
					if (item.getUploadFileKey() != null && item.getUploadFileKey().length() > 0) {
						log.debug("등록성공: " + item.getUploadFileKey() + ", " + item.toString());
						isUploadOrDelete = db_update_file_status(item.getPhysicalPath(), 1, item.getUploadFileKey(),
								item.getContentPath(), item.getSnapshotPath(), item.getChecksumType(),
								item.getPoster());
						if (!isUploadOrDelete) {
							log.warn("등록된 파일 업데이트 실패 : " + item.getUploadFileKey());
						}
					} else {
						log.trace("등록실패: " + item.getUploadFileKey());
						isUploadOrDelete = db_delete_item_pysicalpath(item.getPhysicalPath());
						if (!isUploadOrDelete) {
							log.warn("등록 안된 파일 삭제 실패 : " + item.getUploadFileKey());
						}
					}
				}
			} else {
				log.error("server return != 200 or data error");
				return 0;
			}
			log.info("postWatcherFileRegister End : " + utils.info.getName());
		} finally {
			results.close();
		}

		log.info("items: " + sendItem.toString());

		return sendItem.size();
	}

	/**
	 * path에 해당하는 FILES 아이템의 STATUS를 변경한다.
	 * 
	 * @param stmt   DATABASE Statement
	 * @param path   파일의 물리적 위치
	 * @param status 업데이트할 STATUS 값
	 * @param cid    서버에서 획득한 CID값
	 * @param cpath  서버에서 획득한 CPATH 값
	 * @param poster
	 * @return
	 */
	protected boolean db_update_file_status(String path, int status, String cid, String cpath, String snap_path,
			int checksum_type, Poster poster) throws SQLException {
		if (stmt == null)
			return false;

		int poster_position = poster == null ? -1 : poster.getPosition();
		int poster_width = poster == null ? -1 : poster.getWidth();
		int poster_height = poster == null ? -1 : poster.getHeight();

		String sqlFmt = "UPDATE FILES SET " + " STATUS=%d, CID='%s', CPATH='%s', SNAP_PATH='%s', "
				+ " POSTER_POS=%d, POSTER_WIDTH=%d, POSTER_HEIGHT=%d, CHECKSUM_TYPE=%d " + " WHERE PATH='%s'";

		String sql = String.format(sqlFmt, status, cid, cpath, snap_path, poster_position, poster_width, poster_height,
				checksum_type, path.replace("'", "''"));

		if (stmt.executeUpdate(sql) == 0) {
			return false;
		}

		return true;
	}

	/**
	 * 작업 대상 파일의 리스트를 획득한다. - 비교횟수가 conf에 지정된 횟수보다 크면서 파일의 업데이트 시간이 현재 작업시간과 conf에
	 * 지정된 시간(초) 보다 크게 차이가 나야함. - 파일 확인 작업의 오류 발생시간이 NEW_DATE 보다 작은 경우 NEW_DATE가 반드시
	 * ERR_DATE 보다 커야한다. 오류가 발생한 이후로 사용자가 업데이트를 했어야한다. 파일 조회 시 정렬(Order by)순서를 의미없는
	 * ERR_DATE(이값은 고정값), CHK_DATE(이 값은 매번 실행할때마다 최신값임), PATH인데, 여기서 PATH에 서비스계정명이
	 * 들어가고 해당 명이 뒷순서일 경우 먼저 등록된 파일임에도 불구하고 이름때문에 맨 나중에 검색됨. 따라서 해당 조건(PATH) 삭제 하고
	 * CID(Upload Key)순으로 변경 처리 대상 리스트 조회 시 STATUS가 1인(등록성공인) row만 조회되도록 수정.
	 * copyComplete 처리가 오래걸릴 경우 schedule에 의해 중복 등록이 되는 case가 발생하여 해당 로직을 추가함. 추가로
	 * copyComplete 전송 시(전송 전에) STATUS값을 2로 업데이트 함.
	 * 
	 * @param stmt
	 * @param chk_time
	 * @return
	 */
	protected ArrayList<FileItemDTO> selectCompareOver(String chk_time) throws Exception {
		ArrayList<FileItemDTO> selectItems = new ArrayList<FileItemDTO>();

		String sqlFmt = "SELECT PATH, OLD_SIZE, NEW_SIZE, OLD_DATE, NEW_DATE, COMP_CNT, STATUS, CHK_DATE, CID, CPATH, DOMAIN, UPLOAD_PATH, SNAP_PATH, TITLE, MD5, CHECKSUM_TYPE, POSTER_POS, POSTER_WIDTH, POSTER_HEIGHT "
				+ " FROM FILES WHERE STATUS=1 AND COMP_CNT >= %d AND TIMESTAMPDIFF(SECOND, NEW_DATE, '%s') > %d "
				+ " AND CHK_DATE > ERR_DATE AND CID !='' ORDER BY ERR_DATE, CHK_DATE, CID";

		ResultSet results = null;
		String sql = "";
		try {
			sql = String.format(sqlFmt, utils.info.getCheckinCount(), chk_time, utils.info.getCheckinTime());

			results = stmt.executeQuery(sql);

			int max = utils.conf.getWatcherSendMax();
			int count = 0;
			while (results.next()) {
				String new_date = results.getString("NEW_DATE");
				FileItemDTO item = CommonUtils.fromResultSet(results, "", "", 0, 0, "");

				boolean bExist = item.isExistPhysicalPath();
				if (bExist == true) {
					try {
						Date d = dateFormat.parse(new_date);
						item.setLastModified(d.getTime());
					} catch (ParseException e) {
						log.error(e.toString());
					}

					utils.getMediaContentInfo(item);

					selectItems.add(item);
					count++;
				}

				log.debug(item.toString());

				if (count > max) {
					break;
				}
			}

			/**
			 * selectItems에 포함된 FileItem중 md5정보가 없는 정보에 대한 작업 수행 1. md5 checksum 구하기(md5가
			 * on일 경우에만) 2. 로컬 정보(db) 업데이트하기 3. selectItems에 업데이트 하기
			 */
			for (FileItemDTO item : selectItems) {
				if (item.getChecksumType() == FileItemDTO.MD5) {
					// md5 정보를 구해야하는 상황이면
					if (WatcherUtils.isEmpty(item.getMd5()) == true) {
						item.setMd5(utils.get_file_md5(item.getPhysicalPath()));
						if (update_fileitem_md5(item) == false) {
							log.error("md5 update error: " + item.getPhysicalPath());
						}
						log.debug(String.format("item.path:%s, item.md5:%s", item.getPhysicalPath(), item.getMd5()));
					}
				} else {
					log.debug(String.format("item.path:%s, item.md5 is off", item.getPhysicalPath()));
				}
			}
		} finally {
			results.close();
		}

//		if (selectItems.size() == 0) {
//			System.out.println(sql);
//		}
		return selectItems;
	}

	/**
	 * path로 DB에 등록된 파일 정보를 삭제한다.
	 * 
	 * @param path
	 * @return
	 */
	private boolean db_delete_item_pysicalpath(String path) throws SQLException {
		String sqlFmt = "DELETE FROM FILES WHERE PATH='%s'";
		String sql = String.format(sqlFmt, path.replace("'", "''"));

		if (stmt.executeUpdate(sql) == 0) {
			return false;
		}

		return true;
	}

	/**
	 * local database에 md5 데이터를 추가한다.
	 * 
	 * @param stmt
	 * @param item
	 * @return
	 */
	protected boolean update_fileitem_md5(FileItemDTO item) throws SQLException {

		if (stmt == null)
			return false;

		String sqlFmt = "UPDATE FILES SET MD5='%s' WHERE PATH='%s'";
		String sql = String.format(sqlFmt, item.getMd5(), item.getPhysicalPath().replace("'", "''"));

		if (stmt.executeUpdate(sql) == 0) {
			return false;
		}

		return true;
	}

	/**
	 * watcher.list.complete 경로에 complete 정보를 전송한다.
	 * 
	 * @param stmt
	 * @param url
	 * @param sendItem
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public int sendFtpCompleteApiCnt(SendFileItemsDTO sendItem) throws Exception {
		int cnt = 0;
		KollusApiWatchersDTO apiResult = utils.sendFtpCompleteApi(sendItem);

		for (KollusApiWatcherContentDTO item : apiResult.result.watcher_files) {
			if (item.error == 0) {
				// error가 아닌 경우만 media_content_id가 있으나 사용처가 없어 삭제함
				FileItemDTO findItem = utils.findSendItem(sendItem, item.result.key);
				if (findItem == null) {
					log.error("FileItem  파일 정보를 찾을 수 없습니다. [" + item.result.key + "]");

				}
				db_delete_item_pysicalpath(findItem.getPhysicalPath());
				cnt += 1;
				continue;
			}

			log.error("error code: " + item.error + ", error message: " + item.message);

			if (item.result == null) {
				continue;
			}

			FileItemDTO findItem = utils.findSendItem(sendItem, item.result.key);

			if (findItem == null) {
				log.error("실패한 파일 정보를 찾을 수 없습니다. [" + item.result.key + "]");
				continue;
			}

			// working file 삭제 및 snapshot 삭제
			utils.transcodeCancel(findItem);
			boolean isDeleted = db_delete_item_pysicalpath(findItem.getPhysicalPath());
			if (!isDeleted) {
				log.warn("Fail to deleted data from h2 : " + findItem.getPhysicalPath());
			}
			findItem.setCompleteFail(true);

			/**
			 * error_code가 반환되면 추가 처리함.
			 */
			if (item.result.error_code > 0) {
				utils.sendErrorReport(item.result.error_code, item.result.error_detail, "");
			}

		}
		return cnt;
	}

	/**
	 * 비교는 계속하지만 서버에서 등록을 받아주지 않은 컨텐츠 정보를 삭제한다. 이때 파일이 존재하지 않는 경우만 삭제한다.
	 * 
	 * @param stmt
	 */
	protected void removeNotExistFile() throws SQLException {
		ResultSet results = null;
		ArrayList<String> checkFiles = new ArrayList<String>();
		try {
			String sqlFmt = "SELECT PATH, OLD_SIZE, NEW_SIZE, OLD_DATE, NEW_DATE, COMP_CNT, STATUS, CHK_DATE, CID, CPATH, DOMAIN, UPLOAD_PATH, SNAP_PATH, TITLE "
					+ " FROM FILES WHERE COMP_CNT > 10 AND CID = ''";

			String sql = String.format(sqlFmt);
			results = stmt.executeQuery(sql);

			while (results.next()) {
				String physical_path = results.getString(1);
				File file = new File(physical_path);
//				log.debug(physical_path + " - " + file.getAbsolutePath());
				if (!file.exists()) {
					checkFiles.add(physical_path);
				}
			}

			for (String item : checkFiles) {
				String sqlUpdate = String.format("DELETE FROM FILES WHERE PATH='%s' AND COMP_CNT > 10", item);

				if (stmt.executeUpdate(sqlUpdate) == 0) {
					log.error(sqlUpdate);
				}
			}
		} finally {
			if (results != null) {
				results.close();
			}
		}
	}
}
