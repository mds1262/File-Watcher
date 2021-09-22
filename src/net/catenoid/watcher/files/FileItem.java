package net.catenoid.watcher.files;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.job.Poster;
import net.catenoid.watcher.job.Role_Watcher.ApiResult_WatcherApi.WatcherFile;
import net.catenoid.watcher.utils.*;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.kollus.json_data.BaseCommand;
import com.kollus.utils.myUtils;

public class FileItem implements Comparable {
	
	private static Logger log = Logger.getLogger(FileItem.class);
	private static SimpleDateFormat dateFmt;
	private static String NEW_LINE;
	private static boolean isWindows = false;
	
	private final int 	KIND_PASSTHROUGH = 3;

    public static final int MD5 = 1;

	static {
		dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		NEW_LINE = System.getProperty("line.separator");
		//NEW_LINE = ",\t";
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
	private int watcher_file_kind = WatcherFolder.DEFAULT_WATCHER_FILE_KIND;
	
	@Expose
	@SerializedName("module_key")
	private String module_key;
	
	/**
	 * audio upload를 이용한 경우 1, 기본값  = 0
	 */
	@Expose
	@SerializedName("is_audio_file")
	public int is_audio_file = 0;

    /**
     * md5 체크 여부 0: off, 1: md5
     */
    @Expose
    @SerializedName("checksum_type")
    private int checksum_type = 0;

    public void set_checksum_type(int checksum_type) { this.checksum_type = checksum_type; }
    public int get_checksum_type() { return checksum_type; }
	
	@Expose(serialize = false, deserialize = true)
	private Poster poster;
	
	public Poster getPoster() {
		return poster;
	}
	
	public void setPoster(Poster p) {
		this.poster = p;
	}

	/**
	 * 확장 기능 폴더인지 확인하는 플래그
	 */
	@Deprecated
	private boolean bPassThrough  = false;
	
	@Deprecated
	public boolean isPassThrough() {
		return bPassThrough;
	}

	/**
	 * isPassThrough를 설정한다.
	 * @param passthrough
	 */
	@Deprecated
	private void setPassThrough(boolean passthrough) {
		bPassThrough = passthrough;
	}
	
	/**
	 * 암호화 컨텐츠인지 확인하는 플레그 0이면 일반파일, 1이면 암호화 파일요청
	 */
	@Expose
	@SerializedName("use_encryption")
	private int use_encryption = 0;
	
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
	private String media_profile_key;
	
	/**
	 * 파일명 (전체경로 포함)
	 * Watcher의 전체 경로를 포함함다. - 실제 물리적인 경로
	 */
	@Expose
	@SerializedName("physical_path")
	private String physical_path;
	
	public String get_physical_path() {
		return physical_path;
	}
	
	public void set_physical_path(String path) {
		physical_path = path;
	}
	
	/**
	 * 파일명 (업로드 완료 폴더 하위 경로, 도메인 경로 포함) <BR>
	 * watcher_files.upload_path
	 */
	@Expose
	@SerializedName("upload_path")
	private String upload_path;
	
	public String get_upload_path() {
		return upload_path;
	}
	
	/**
	 * 파일크기 watcher_files.filesize
	 */
	@Expose
	@SerializedName("filesize")
	private long filesize;
	
	public long get_filesize() {
		return filesize;
	}
	
	@Expose
	@SerializedName("filesize_str")
	private String filesize_str;
	
	/**
	 * 최종수정일
	 */
	@Expose
	@SerializedName("lastmodified")
	private long last_modified;
	
	public long get_last_modified() {
		return last_modified;
	}
	
	public void set_last_modified(long time) {
		last_modified = time;
	}

	@Expose
	@SerializedName("lastmodified_str")
	private String last_modified_str;
	
	/**
	 * 미디어 파일 정보 
	 */
	@Expose
	@SerializedName("media_information")
	private String media_information;
	
	public ContentInfo mediaInfo;
//	public MediaInformation mediaInfo;
	
	/**
	 * 고객의 DOMAIN명 (서비스 도메인)
	 */
	@Expose
	@SerializedName("content_provider_key")
	public String content_provider_key;
	
	/**
	 * watcher_files.key
	 */
	@Expose
	@SerializedName("key")
	private String upload_file_key;
	
	public String get_upload_file_key() {
		return upload_file_key;
	}
	
	public void set_upload_file_key(String key) {
		this.upload_file_key = key;
	}
	
	/**
	 * watcher_files.content_path <br>
	 * CMS에 인식하는 컨텐츠 경로  <br>
	 * /[content_provider_key]/~  <br>
	 */
	@Expose
	@SerializedName("content_path")
	private String content_path;
	
	public String get_content_path() {
		return this.content_path;
	}
	
	public void set_content_path(String path) {
		this.content_path = path;
	}
	
	@Expose
	@SerializedName("check.date")
	private String check_date;
	
	/**
	 * SNAP 파일 경로 <BR>
	 * wathcer_files.snapshot_path
	 */
	@Expose
	@SerializedName("snapshot_path")
	private String snapshot_path;
	
	public String get_snapshot_path() {
		return snapshot_path;
	}
	
	public String get_poster_path() {
		return snapshot_path;
	}
	
	public void set_snapshot_path(String path) {
		this.snapshot_path = path;
	}
	
	public void set_poster_path(String path) {
		this.snapshot_path = path;
	}

	/**
	 * watcher_files.title
	 */
	@Expose
	@SerializedName("title")
	private String title;
	
	public String get_title() {
		return this.title;
	}
	
	/**
	 * original file md5 checksum
	 */
	@Expose
	@SerializedName("md5")
	private String md5;
	
	public String get_md5() {
		return this.md5;
	}
	
	public void set_md5(String md5sum) {
		this.md5 = md5sum;
	}


	@Expose
	@SerializedName("format")
	private String format = "Unknown";
	
	/**
	 * working 폴더에 파일을 복사 성공시 true로 설정된다.
	 * 복사 작업 이후에만 true로 설정
	 */
	private boolean copy_complete = false;
	
	public boolean is_copy_complete() {
		return this.copy_complete;
	}
	
	public void set_copy_complete(boolean complete) {
		this.copy_complete = complete;
	}
	
	public String getDateString() {
		return myUtils.getDateString(last_modified);
	}
	
	/**
	 * 임의로 FileItem을 생성할 수 없도록 한다.
	 */
	private FileItem() {
		
	}

	public String toString() {	    
	    Date d = new Date();
	    d.setTime(last_modified);
	    
	    this.filesize_str = readableFileSize(filesize);
	    this.last_modified_str = dateFmt.format(d);
	    
	    if(this.isPassThrough() == true) {
	    	this.watcher_file_kind = KIND_PASSTHROUGH;
	    }
	    
	    /**
	     * category 변수가 "" 일때 제거.
	     */
	    if(this.category != null && this.category.compareTo("") == 0) {
	    	this.category = null;
	    }
	    
	    if(this.mediaInfo != null) {
		    try {
				this.media_information = this.mediaInfo.toJSONEncodedString();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				log.error(e);
			}
	    }
	    
	    final Gson gson = BaseCommand.gson(false);
		return gson.toJson(this);
	}
	
	/**
	 * FileItem을 생성하는 함수
	 * lineParser에서 사용되는 함수입니다.
	 * @param watcher_dir
	 * @param filename
	 * @param lastModified
	 * @param length
	 * @return
	 */
	public static FileItem fromLineParser(String watcher_dir, String filename, long lastModified, long length) {
		FileItem item = new FileItem();
		item.physical_path = filename;
		item.filesize = length;
		item.last_modified = lastModified;
		item.content_provider_key = myUtils.getDomain(watcher_dir, item.physical_path);
		item.upload_path = myUtils.getUploadPath(watcher_dir, item.physical_path);
		String ext = myUtils.getFileExtention(item.upload_path);
		item.title = myUtils.getWithoutDomain(watcher_dir, item.upload_path);
		item.title = item.title.substring(0, item.title.length() - ext.length()); 
		return item;
	}
	
	/**
	 *  watcher_file/register의 반환값을 FileItem으로 생성한다.<br>
	 * 확인하는 tag 리스트 <br>
	 * 	- error<br>
	 * 	- message : error가 0이 아닌 경우<br>
	 *  - result { physical_path, key, content_path, snapshot_path }
	 */
	public static FileItem fromApiResult(WatcherFile item) {
		Gson gson = BaseCommand.gson(false);
		String json = gson.toJson(item);
		log.debug("register 반환값 (fileItem) = "+ json);
		ApiResult_WatcherFileRegister r = gson.fromJson(json, ApiResult_WatcherFileRegister.class); 
		return r.result;
	}

	/**
	 * watcher_file/register의 반환값을 FileItem으로 생성한다.<br>
	 * 확인하는 tag 리스트 <br>
	 * - error<br>
	 * - message : error가 0이 아닌 경우<br>
	 * - result { physical_path, key, content_path, snapshot_path }
	 */
	public class ApiResult_WatcherFileRegister {
		@Expose
		public int error;
		
		@Expose
		public String message;
		
		@Expose
		public FileItem result;
	}
	
	/**
	 * this.pysical_path에 물리적인 파일이 존재하는지 확인한다.
	 * @return
	 */
	public boolean isExistPhysicalPath() {
		if(this.physical_path == null || this.physical_path.isEmpty()) {
			return false;
		}
		File f = new File(this.physical_path);
		return f.exists();
	}
	
	public static FileItem fromResultSet(ResultSet results) throws SQLException {
		
		String physical_path = results.getString("PATH");
		String old_size = results.getString("OLD_SIZE");
		long   new_size = results.getLong("NEW_SIZE");
		String old_date = results.getString("OLD_DATE");
		String new_date = results.getString("NEW_DATE");
		String comp_cnt = results.getString("COMP_CNT");
		String status = results.getString("STATUS");
		String chk_date = results.getString("CHK_DATE");
		String key = results.getString("CID");
		String content_path = results.getString("CPATH");
		String content_provider_key = results.getString("DOMAIN");
		String upload_path = results.getString("UPLOAD_PATH");
		String snapshot_path = results.getString("SNAP_PATH");
		String title = results.getString("TITLE");
		String md5 = results.getString("MD5"); // MD5 추가
        int checksum_type = results.getInt("CHECKSUM_TYPE"); // CHECKSUM_TYPE 추가
		int poster_position = results.getInt("POSTER_POS");
		int poster_width = results.getInt("POSTER_WIDTH");
		int poster_height = results.getInt("POSTER_HEIGHT");
		
		ExtFilenameResult filename = ExtFilenameParser.Parser(upload_path);			
		FileItem item = new FileItem();
		
		item.physical_path = physical_path;
		item.filesize = new_size;
		item.upload_file_key = key;
		item.content_path = content_path;
		item.content_provider_key = content_provider_key;
		item.snapshot_path = snapshot_path;
		item.md5 = md5;
        item.checksum_type = checksum_type;
		item.setPoster(new Poster(poster_position, poster_width, poster_height));

		item.category = filename.getCategory();
		item.media_profile_key = filename.getMediaProfileKey();
		item.title = filename.getTitle();
		item.upload_path = filename.getUploadPath();
		item.use_encryption = filename.isEncryptPath() ? 1 : 0;
		item.is_audio_file = filename.isAudioPath() ? 1 : 0;
		item.setPassThrough(filename.isPassthroughPath());
			
		log.debug(item.upload_path);
		log.debug(item.toString());
		
		return item;
	}
	
	/**
	 * 파일의 크기를 읽기 편한 단위로 변환한다.
	 * @param size
	 * @return
	 */
	private static String readableFileSize(long size) {
	    if(size <= 0) return "0";
	    final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
	    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
	    return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
	
	public class ContentInfo {
		
		/**
		 * container format
		 */
		@Expose
		@SerializedName("format")
		public String format = "Unkonwn";
		
		@Expose
		@SerializedName("duration")
		public String duration;
		
		/**
		 * Video Info
		 */
		@Expose
		@SerializedName("video.format")
		public String videoFormat;
		
		@Expose
		@SerializedName("video.duration")
		public String videoDuration;
		
		@Expose
		@SerializedName("video.codec")
		public String videoCodec;
		
		@Expose
		@SerializedName("video.bitrate")
		public String videoBitrate;
		
		@Expose
		@SerializedName("video.width")
		public String videoWidth;
		
		@Expose
		@SerializedName("video.height")
		public String videoHeight;
		
		@Expose
		@SerializedName("video.framerate")
		public String videoFrameRate;
		
		@Expose
		@SerializedName("video.ratio")
		public String videoRatio;
		
		@Expose
		@SerializedName("video.rotation")
		private String rotation;

		public String getRotation() {
			return rotation;
		}

		public void setRotation(String rotation) {
			if("".equals(rotation) || rotation==null) rotation="0";
			this.rotation = rotation;
		}

		@Expose
		@SerializedName("video.scantype")
		public String scanType;

		/**
		 * Audio Info
		 */
		@Expose
		@SerializedName("audio.format")
		public String audioFormat;

		@Expose
		@SerializedName("audio.codec")
		public String audioCodec;
		
		@Expose
		@SerializedName("audio.bitrate")
		public String audioBitrate;
		
		@Expose
		@SerializedName("audio.sample.rate")
		public String audioSampleRate;
		
		@Expose
		@SerializedName("audio.duration")
		public String audioDuration;
		
		/**
		 * Image Info
		 */
		@Expose
		@SerializedName("image.format")
		public String imageFormat;
		
		@Expose
		@SerializedName("image.width")
		public String imageWidth;
		
		@Expose
		@SerializedName("image.height")
		public String imageHeight;
		
		
		public String toString() {
			final Gson gson = BaseCommand.gson(true);
			return gson.toJson(this);
		}
				
		public String toJSONEncodedString() throws UnsupportedEncodingException {
			String str = this.toString();
			return URLEncoder.encode(str, "UTF-8");
		}
	}
	
	/**
	 * FileItem의 ContentInfo정보를 획득한다.
	 * 실패하는 경우 mediaInfo는 null 값을 갖는다.
	 * @return
	 */
	public boolean get_media_info() {
	    if(this.mediaInfo == null) {
	    	this.mediaInfo = new ContentInfo();
	    }
	    boolean bRet = get_media_info(this);
	    if(bRet == false) {
	    	this.mediaInfo = null;
	    } else {
	    	this.format = this.mediaInfo.format;
	    }
	    return bRet;
	}
	
	private static String empty2null(String s) {
		if(myUtils.isEmpty(s) == true) {
			return null;
		}
		return s;
	}
	
	private static boolean get_media_info(FileItem f) {
		
		MediaInfo MI = new MediaInfo();

	    if(MI.Open(f.physical_path) == 0) {
	    	log.error("MI.Open - file was not not opened: " + f.physical_path);
	    	return true;
	    }

	    MI.Option("Complete", "");
	    
	    f.mediaInfo.format = empty2null(MI.Get(MediaInfo.StreamKind.General, 0, "Format"));
	    f.mediaInfo.duration = empty2null(MI.Get(MediaInfo.StreamKind.General, 0, "Duration"));
	    
	    f.mediaInfo.videoDuration = empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "Duration"));
	    f.mediaInfo.videoFormat = empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "Format"));
	    f.mediaInfo.videoCodec = empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "CodecID/Hint"));
	    f.mediaInfo.videoBitrate = empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "BitRate"));
	    f.mediaInfo.videoWidth = empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "Width"));
	    f.mediaInfo.videoHeight = empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "Height"));
	    f.mediaInfo.videoFrameRate = empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "FrameRate"));
	    f.mediaInfo.videoRatio = empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "DisplayAspectRatio"));
	    f.mediaInfo.setRotation(empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "Rotation")));
	    
	    /**
	     * Video scanType : Interlaced
	     */
	    f.mediaInfo.scanType = empty2null(MI.Get(MediaInfo.StreamKind.Video, 0, "ScanType/String"));
	    
	    f.mediaInfo.audioFormat = empty2null(MI.Get(MediaInfo.StreamKind.Audio, 0, "Format"));
	    f.mediaInfo.audioCodec = empty2null(MI.Get(MediaInfo.StreamKind.Audio, 0, "CodecID/Hint"));
	    f.mediaInfo.audioBitrate = empty2null(MI.Get(MediaInfo.StreamKind.Audio, 0, "BitRate"));
	    f.mediaInfo.audioSampleRate = empty2null(MI.Get(MediaInfo.StreamKind.Audio, 0, "SamplingRate"));
	    f.mediaInfo.audioDuration = empty2null(MI.Get(MediaInfo.StreamKind.Audio, 0, "Duration"));
	    
	    f.mediaInfo.imageFormat = empty2null(MI.Get(MediaInfo.StreamKind.Image, 0, "Format"));
	    f.mediaInfo.imageWidth = empty2null(MI.Get(MediaInfo.StreamKind.Image, 0, "Width"));
	    f.mediaInfo.imageHeight = empty2null(MI.Get(MediaInfo.StreamKind.Image, 0, "Height"));

	    MI.Close();

		return true;		
	}

	/**
	 * 동일한 경우만 0 이며, -1과 1은 의미 없음 
	 */
	@Override
	public int compareTo(Object obj) {
		/**
		 * 파일명이 동일한 경우 length, date 비교
		 */
		if(obj instanceof FileItem) {
			FileItem i = (FileItem) obj;
			int cmp = this.physical_path.compareTo(i.physical_path);
			if(cmp == 0) {
				if(this.filesize == i.filesize && this.last_modified == i.last_modified) {
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
	 * 파일의 lastModified 시각과 현재시간(cur_time)의 차이가 checkinTime이상 차이나면 true
	 * 파일이 감시 대상에서 work 대상으로 이동 시키기 전에 판단하는 함수
	 * @param cur_time
	 * @param checkinTime
	 * @return
	 */
	public boolean isOverTime(long cur_time, long checkinTime) {
		return (cur_time - this.last_modified) >= checkinTime;
	}

	public boolean checkSupportExt() {

		File f = new File(this.physical_path);
	    int idx = f.getName().lastIndexOf('.');
	    String ext =  ((idx > 0) ? f.getName().substring(idx) : "");
	    
	    try {
			for(String i : Config.getConfig().getSupportExt()) {
				if(i.length() > 0 && ext.compareToIgnoreCase(i) == 0) {
					return true;
				}
			}
		} catch (Exception e) {
			log.error(LogAction.CONFIG_ERROR + e.toString());			
		}
	    
	    return false;
	}

	// cms에 파일 complete 실패하면 true로 설정됨.
	public boolean complete_fail = false;
	
	/**
	 * API에서 할당한 Poster 정보
	 */
//	private Poster poster;
	
	/**
	 * passthrough가 설정된 상태에서 외부에서 kind를 1(ftp_upload), 2(http_upload)로 변경하면 안되기 때문에 <br>
	 * 이미 passthrough이면 kind를 변경하지 안도록 한다.
	 * @param watcher_file_kind
	 */
	public void set_watcher_file_kind(int watcher_file_kind) {
		if(this.watcher_file_kind != KIND_PASSTHROUGH) {
			this.watcher_file_kind = watcher_file_kind;
		}
	}

	public void set_module_key(String module_key) {
		this.module_key = module_key;
	}

}
