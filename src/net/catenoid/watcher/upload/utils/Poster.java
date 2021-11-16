package net.catenoid.watcher.upload.utils;

import com.google.gson.annotations.Expose;

public class Poster {
	
	/**
	 * 
	 * @param position
	 * @param width
	 * @param height
	 */
	public Poster(int position, int w, int h) {
		this.second = position;
		this.width = w;
		this.height = h;
	}
	
	/**
	 * Poster 파일 생성 위치 (DURATION)
	 */
	@Expose
	private int second = -1;
	
	/**
	 * Poster 파일 폭
	 */
	@Expose
	private int width = -1;
	
	/**
	 * Poster 파일 높이
	 */
	@Expose
	private int height = -1;
	
	public int getPosition() {
		return second;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
}