package net.catenoid.watcher.upload.dto;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.kollus.json_data.BaseCommand;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.upload.utils.Poster;
import net.catenoid.watcher.utils.WatcherUtils;

public class FileItemDTO implements Comparable<FileItemDTO> {

	private static Logger log = Logger.getLogger(FileItemDTO.class);
	private static SimpleDateFormat dateFmt;
	@SuppressWarnings("unused")
	private static String NEW_LINE;
	@SuppressWarnings("unused")
	private static boolean isWindows = false;

	public static final int KIND_PASSTHROUGH = 3;

	public static final int MD5 = 1;

	static {
		dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		NEW_LINE = System.getProperty("line.separator");
		// NEW_LINE = ",\t";
		String os = System.getProperty("os.name");
		if (os != null && os.toLowerCase().startsWith("windows")) {
			isWindows = true;
		}
	}

	/**
	 * 폴더종류, 1:ftp업로드 폴더, 2:http 업로드 폴더, 3:passthrough (기본값:1)
	 */
	@Expose
	@SerializedName("kind")
	private int watcherFileKind = WatcherFolder.DEFAULT_WATCHER_FILE_KIND;

	@Expose
	@SerializedName("module_key")
	private String moduleKey;

	/**
	 * audio upload를 이용한 경우 1, 기본값 = 0
	 */
	@Expose
	@SerializedName("is_audio_file")
	public int isAudioFile = 0;

	/**
	 * md5 체크 여부 0: off, 1: md5
	 */
	@Expose
	@SerializedName("checksum_type")
	private int checksumType = 0;

	@Expose(serialize = false, deserialize = true)
	private Poster poster;

	/**
	 * 암호화 컨텐츠인지 확인하는 플레그 0이면 일반파일, 1이면 암호화 파일요청
	 */
	@Expose
	@SerializedName("use_encryption")
	private int useEncryption = 0;

	/**
	 * passthrough 옵션일때 category를 설정할 수 있음.
	 */
	@Expose
	@SerializedName("category")
	private String category;

	/**
	 * 확장기능 폴더의 경우 파일명에서 미디어 인코딩 프로파일키를 획득하여 설정한다.
	 */
	@Expose
	@SerializedName("media_profile_key")
	private String mediaProfileKey;

	/**
	 * 확장기능 폴더의 경우 파일명에서 미디어 인코딩 프로파일키를 획득하여 설정한다.
	 */
	@Expose
	@SerializedName("tr_media_profile_key")
	private String trMediaProfileKey;

	/**
	 * 파일명 (전체경로 포함) Watcher의 전체 경로를 포함함다. - 실제 물리적인 경로
	 */
	@Expose
	@SerializedName("physical_path")
	private String physicalPath;

	/**
	 * 파일명 (업로드 완료 폴더 하위 경로, 도메인 경로 포함) <BR>
	 * watcher_files.upload_path
	 */
	@Expose
	@SerializedName("upload_path")
	private String uploadPath;

	/**
	 * 파일크기 watcher_files.filesize
	 */
	@Expose
	@SerializedName("filesize")
	private long filesize;

	@Expose
	@SerializedName("filesize_str")
	private String filesizeStr;

	/**
	 * 최종수정일
	 */
	@Expose
	@SerializedName("lastmodified")
	private long lastModified;

	@Expose
	@SerializedName("lastmodified_str")
	private String lastModifiedStr;

	/**
	 * 미디어 파일 정보
	 */
	@Expose
	@SerializedName("media_information")
	private String mediaInformation;

	/**
	 * 고객의 DOMAIN명 (서비스 도메인)
	 */
	@Expose
	@SerializedName("content_provider_key")
	private String contentProviderKey;

	/**
	 * watcher_files.key
	 */
	@Expose
	@SerializedName("key")
	private String uploadFileKey;

	/**
	 * watcher_files.content_path <br>
	 * CMS에 인식하는 컨텐츠 경로 <br>
	 * /[content_provider_key]/~ <br>
	 */
	@Expose
	@SerializedName("content_path")
	private String contentPath;

	@Expose
	@SerializedName("check.date")
	private String checkDate;

	/**
	 * SNAP 파일 경로 <BR>
	 * wathcer_files.snapshot_path
	 */
	@Expose
	@SerializedName("snapshot_path")
	private String snapshotPath;

	/**
	 * watcher_files.title
	 */
	@Expose
	@SerializedName("title")
	private String title;

	/**
	 * original file md5 checksum
	 */
	@Expose
	@SerializedName("md5")
	private String md5;

	@Expose
	@SerializedName("format")
	public String format = "Unknown";

	/**
	 * Passthrough TransCoding 선진행 하기위해 "@"을 가지고 확인한다
	 */
	@Expose
	@SerializedName("passthrough_ahead")
	private int passthrough_ahead = 0;
	
	@Expose
	@SerializedName("channel_keys")
	private String channelKeys = "";

	private ContentInfoDTO mediaInfo;

	/**
	 * working 폴더에 파일을 복사 성공시 true로 설정된다. 복사 작업 이후에만 true로 설정
	 */
	private boolean copyComplete = false;

	// cms에 파일 complete 실패하면 true로 설정됨.
	private boolean completeFail = false;

	private boolean isConsoleUpload = false;

	public int getWatcherFileKind() {
		return watcherFileKind;
	}

	/**
	 * passthrough가 설정된 상태에서 외부에서 kind를 1(ftp_upload), 2(http_upload)로 변경하면 안되기 때문에
	 * <br>
	 * 이미 passthrough이면 kind를 변경하지 안도록 한다.
	 * 
	 * @param watcher_file_kind
	 */
	public void setWatcherFileKind(int watcherFileKind) {
		if (this.watcherFileKind != KIND_PASSTHROUGH) {
			this.watcherFileKind = watcherFileKind;
		}
	}

	public String getModuleKey() {
		return moduleKey;
	}

	public void setModuleKey(String moduleKey) {
		this.moduleKey = moduleKey;
	}

	public int getIsAudioFile() {
		return isAudioFile;
	}

	public void setIsAudioFile(int isAudioFile) {
		this.isAudioFile = isAudioFile;
	}

	public int getChecksumType() {
		return checksumType;
	}

	public void setChecksumType(int checksumType) {
		this.checksumType = checksumType;
	}

	public Poster getPoster() {
		return poster;
	}

	public void setPoster(Poster poster) {
		this.poster = poster;
	}

	public int getUseEncryption() {
		return useEncryption;
	}

	public void setUseEncryption(int useEncryption) {
		this.useEncryption = useEncryption;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getMediaProfileKey() {
		return mediaProfileKey;
	}

	public void setMediaProfileKey(String mediaProfileKey) {
		this.mediaProfileKey = mediaProfileKey;
	}

	public String getTrMediaProfileKey() {
		return trMediaProfileKey;
	}

	public void setTrMediaProfileKey(String trMediaProfileKey) {
		this.trMediaProfileKey = trMediaProfileKey;
	}

	public String getPhysicalPath() {
		return physicalPath;
	}

	public void setPhysicalPath(String physicalPath) {
		this.physicalPath = physicalPath;
	}

	public String getUploadPath() {
		return uploadPath;
	}

	public void setUploadPath(String uploadPath) {
		this.uploadPath = uploadPath;
	}

	public long getFilesize() {
		return filesize;
	}

	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}

	public String getFilesizeStr() {
		return filesizeStr;
	}

	public void setFilesizeStr(String filesizeStr) {
		this.filesizeStr = filesizeStr;
	}

	public long getLastModified() {
		return this.lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public String getLastModifiedStr() {
		return lastModifiedStr;
	}

	public void setLastModifiedStr(String lastModifiedStr) {
		this.lastModifiedStr = lastModifiedStr;
	}

	public String getMediaInformation() {
		return mediaInformation;
	}

	public void setMediaInformation(String mediaInformation) {
		this.mediaInformation = mediaInformation;
	}

	public String getContentProviderKey() {
		return contentProviderKey;
	}

	public void setContentProviderKey(String contentProviderKey) {
		this.contentProviderKey = contentProviderKey;
	}

	public String getUploadFileKey() {
		return uploadFileKey;
	}

	public void setUploadFileKey(String uploadFileKey) {
		this.uploadFileKey = uploadFileKey;
	}

	public String getContentPath() {
		return contentPath;
	}

	public void setContentPath(String contentPath) {
		this.contentPath = contentPath;
	}

	public String getCheckDate() {
		return checkDate;
	}

	public void setCheckDate(String checkDate) {
		this.checkDate = checkDate;
	}

	public String getSnapshotPath() {
		return snapshotPath;
	}

	public void setSnapshotPath(String snapshotPath) {
		this.snapshotPath = snapshotPath;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public ContentInfoDTO getMediaInfo() {
		return mediaInfo;
	}

	public void setMediaInfo(ContentInfoDTO mediaInfo) {
		this.mediaInfo = mediaInfo;
	}

	public boolean isCopyComplete() {
		return copyComplete;
	}

	public void setCopyComplete(boolean copyComplete) {
		this.copyComplete = copyComplete;
	}

	public boolean isCompleteFail() {
		return completeFail;
	}

	public void setCompleteFail(boolean completeFail) {
		this.completeFail = completeFail;
	}

	public String getDateString() {
		return WatcherUtils.getDateString(this.lastModified);
	}

	public boolean isConsoleUpload() {
		return isConsoleUpload;
	}

	public void setConsoleUpload(boolean isConsoleUpload) {
		this.isConsoleUpload = isConsoleUpload;
	}

	public int getPassthrough_ahead() {
		return passthrough_ahead;
	}

	public void setPassthrough_ahead(int passthrough_ahead) {
		this.passthrough_ahead = passthrough_ahead;
	}
	
	
	public String getChannelKeys() {
		return channelKeys;
	}

	public void setChannelKeys(String channelKeys) {
		this.channelKeys = channelKeys;
	}

	/**
	 * 임의로 FileItem을 생성할 수 없도록 한다.
	 */
	public FileItemDTO() {

	}

	public String toString() {
		Date d = new Date();
		d.setTime(getLastModified());
//	    d.setTime(this.lastModified);

		setFilesizeStr(readableFileSize(filesize));
		setLastModifiedStr(dateFmt.format(d));


		/**
		 * category 변수가 "" 일때 제거.
		 */
		if (getCategory() != null && getCategory().compareTo("") == 0) {
			setCategory(null);
		}

		if (getMediaInfo() != null) {
			try {
				setMediaInformation(getMediaInfo().toJSONEncodedString());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				log.error(e);
			}
		}

		final Gson gson = BaseCommand.gson(false);
		return gson.toJson(this);
	}

	/**
	 * watcher_file/register의 반환값을 FileItem으로 생성한다.<br>
	 * 확인하는 tag 리스트 <br>
	 * - error<br>
	 * - message : error가 0이 아닌 경우<br>
	 * - result { physical_path, key, content_path, snapshot_path }
	 */
//	public static FileItemDTO fromApiResult(KollusApiWatcherContentDTO item) {
//		Gson gson = BaseCommand.gson(false);
//		String json = gson.toJson(item);
//		log.debug("register 반환값 (fileItem) = " + json);
//		ApiResult_WatcherFileRegister r = gson.fromJson(json, ApiResult_WatcherFileRegister.class);
//		return r.result;
//	}

	/**
	 * watcher_file/register의 반환값을 FileItem으로 생성한다.<br>
	 * 확인하는 tag 리스트 <br>
	 * - error<br>
	 * - message : error가 0이 아닌 경우<br>
	 * - result { physical_path, key, content_path, snapshot_path }
	 */
//	public class ApiResult_WatcherFileRegister {
//		@Expose
//		public int error;
//
//		@Expose
//		public String message;
//
//		@Expose
//		public FileItemDTO result;
//	}

	/**
	 * this.pysical_path에 물리적인 파일이 존재하는지 확인한다.
	 * 
	 * @return
	 */
	public boolean isExistPhysicalPath() {
		if (this.physicalPath == null || this.physicalPath.isEmpty()) {
			return false;
		}
		File f = new File(this.physicalPath);
		return f.exists();
	}

	/**
	 * 파일의 크기를 읽기 편한 단위로 변환한다.
	 * 
	 * @param size
	 * @return
	 */
	private static String readableFileSize(long size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	/**
	 * 동일한 경우만 0 이며, -1과 1은 의미 없음
	 */
	@Override
	public int compareTo(FileItemDTO obj) {
		/**
		 * 파일명이 동일한 경우 length, date 비교
		 */
		if (obj instanceof FileItemDTO) {
			FileItemDTO i = (FileItemDTO) obj;
			int cmp = this.physicalPath.compareTo(i.physicalPath);
			if (cmp == 0) {
				if (this.filesize == i.filesize && this.lastModified == i.lastModified) {
					return 0;
				}
				return 1;
			}
			return cmp;
		}

		log.error("비교 불가능한 데이터로 비교되었습니다. FileItem.compareTo");
		return -1;
	}

	/**
	 * 파일의 lastModified 시각과 현재시간(cur_time)의 차이가 checkinTime이상 차이나면 true 파일이 감시 대상에서
	 * work 대상으로 이동 시키기 전에 판단하는 함수
	 * 
	 * @param cur_time
	 * @param checkinTime
	 * @return
	 */
//	public boolean isOverTime(long cur_time, long checkinTime) {
//		return (cur_time - this.lastModified) >= checkinTime;
//	}

	public boolean checkSupportExt() {

		File f = new File(this.physicalPath);
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
}
