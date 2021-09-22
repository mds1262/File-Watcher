package net.catenoid.watcher.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WatcherFolder
{
	public static final int DEFAULT_WATCHER_FILE_KIND = 1;
	
	@Expose
	private String name;
	
	@Expose
	@SerializedName("api_key")
	private String apikey;

	@Expose
	@SerializedName("api_reference")
	private String api_reference;

	@Expose
	@SerializedName("watcher_file.kind")
	private int watcher_file_kind = DEFAULT_WATCHER_FILE_KIND;

	@Expose
	@SerializedName("watcher.dir")
	private String watcher_dir;

	@Expose
	@SerializedName("work.dir")
	private String work_dir;

	@Expose
	@SerializedName("interval")
	private String interval;

	@Expose
	@SerializedName("checkin.time")
	private int checkin_time;

	@Expose
	@SerializedName("checkin.count")
	private int checkin_count;
	
	@Expose
	@SerializedName("delete.diff.minute")
	private long delete_diff;
	
	@Expose
	@SerializedName("monitor.check.limit.minute")
	private long monitor_limit_minute;

	@Expose
	@SerializedName("monitor.check.limit.count")
	private int monitor_limit_count;
	
	@Expose
	@SerializedName("enabled")
	private boolean enabled;
	
	@Expose
	@SerializedName("work.path.prefix")
	private String work_path_prefix = "";
		
	/**
	 * Watcher 모듈 구분명<br>
	 * 서버에 등록된 이름과 동일하도록 한다.
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Wathcer 모듈 구분명 설정
	 * @param name
	 */
	private void setName(String name) {
		this.name = name;
	}
	
	/**
	 * API키 값 획득
	 * @return
	 */
	public String getApiKey() {
		return apikey;
	}
	
	/**
	 * API키 값 설정
	 * @param apikey
	 */
	private void setApikey(String apikey) {
		this.apikey = apikey;
	}
	
	/**
	 * 감시폴더 경로
	 * @return
	 */
	public String getWatcherDir() {
		return watcher_dir;
	}
	
	/**
	 * 감시폴더 경로
	 * @param watcher_dir
	 */
	private void setWatcherDir(String watcher_dir) {
		this.watcher_dir = watcher_dir;
	}
	
	/**
	 * 작업 폴더 경로
	 * @return
	 */
	public String getWorkDir() {
		return work_dir;
	}
	
	/**
	 * 작업 폴더 경로
	 * @param work_dir
	 */
	private void setWorkDir(String work_dir) {
		this.work_dir = work_dir;
	}
	
	/**
	 * 감시 주기 crontab
	 * @return
	 */
	public String getInterval() {
		return interval;
	}
	
	/**
	 * 감시 주기 crontab
	 * @param interval
	 */
	private void setInterval(String interval) {
		this.interval = interval;
	}
	
	/**
	 * 파일 마지막 변화 시간이 현재 시간과 checkin_time 이상 차이나면 작업대상 파일인지 확인한다.
	 * @return
	 */
	public int getCheckinTime() {
		return checkin_time;
	}
	
	private void setCheckinTime(int checkin_time) {
		this.checkin_time = checkin_time;
	}
		
	/**
	 * 작업에 등록될 트리거명
	 * @return
	 */
	public String getTriggerName() {
		return "trigger_" + getName();
	}
	
	/**
	 * 작업에 등록될 그룹명
	 * @return
	 */
	public String getGroupName() {
		return "group_" + getName();
	}

	/**
	 * 작업에 등록될 작업명
	 * @return
	 */
	public String getJobName() {
		return "job_" + getName();
	}

	/**
	 * 파일 상태가 변화하지 않는 상태로 지정 횟수가 되면 work 상태로 변화 시킨다.
	 * @return
	 */
	public int getCheckinCount() {
		return checkin_count;
	}

	private void setCheckinCount(int checkin_count) {
		this.checkin_count = checkin_count;
	}

	public long getDeleteDiff() {
		return delete_diff;
	}

	private void setDeleteDiff(long delete_diff) {
		this.delete_diff = delete_diff;
	}

	public long getMonitorLimitMinute() {
		return monitor_limit_minute;
	}

	private void setMonitorLimitMinute(long monitor_limit_minute) {
		this.monitor_limit_minute = monitor_limit_minute;
	}

	public int getMonitorLimitCount() {
		return monitor_limit_count;
	}

	private void setMonitorLimitCount(int monitor_limit_count) {
		this.monitor_limit_count = monitor_limit_count;
	}

	public boolean isEnabled() {
		return enabled;
	}

	private void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getWorkPathPrefix() {
		return work_path_prefix;
	}

	private void setWorkPathPrefix(String work_path_prefix) {
		this.work_path_prefix = work_path_prefix;
	}

	public String getApiReference() {
		return api_reference;
	}

	private void setApiReference(String api_reference) {
		this.api_reference = api_reference;
	}

	public int getWatcherFileKind() {
		return watcher_file_kind;
	}

	private void setWatcherFileKind(int watcher_file_kind) {
		this.watcher_file_kind = watcher_file_kind;
	}


}

