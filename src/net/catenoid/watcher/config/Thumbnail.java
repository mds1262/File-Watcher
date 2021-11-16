package net.catenoid.watcher.config;

import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.catenoid.watcher.upload.utils.Poster;

public class Thumbnail {
	
	/**
	 * SNAP을 저장할 최종 저장 PATH
	 */
	@Expose
	@SerializedName("snap.dir")
	private String snap_dir;
	
	/**
	 * 정상 파일인지 확인을 위해 SNAP파일을 만들때 사용할 임시 저장 공간
	 */
	@Expose
	@SerializedName("snap.temp")
	private String snap_temp;
	
	@Expose
	private Map<String,Poster> custom;
	
	public Poster get_custom_snapshot(String content_provider_key) {
		if(this.custom == null) {
			return null;
		}
		return custom.get(content_provider_key);
	}
	
	/**
	 * SNAP 파일 생성 위치 (DURATION)
	 */
	@Expose
	@SerializedName("snap.second")
	private int snap_second;
	
	/**
	 * SNAP 파일 폭
	 */
	@Expose
	@SerializedName("snap.width")
	private int width;
	
	/**
	 * SNAP 파일 높이
	 */
	@Expose
	@SerializedName("snap.height")
	private int height;
	
	public int getSnapSecond() {
		return snap_second;
	}

	public void setSnapSecond(int snap_second) {
		this.snap_second = snap_second;
	}

	/**
	 * SNAP 파일 생성에 사용할 임시 폴더
	 * @return
	 */
	public String getSnapTempDir() {
		return snap_temp;
	}

	/**
	 * SNAP 파일 생성에 사용할 임시 폴더
	 * @param snap_temp
	 */
	public void setSnapTempDir(String snap_temp) {
		this.snap_temp = snap_temp;
	}

	public String getSnapDir() {
		return snap_dir;
	}
	
	public void setSnapDir(String snap_dir) {
		this.snap_dir = snap_dir;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
	
	public String sizeToString() {
		return String.format("%d*%d", this.width, this.height);
	}
}
