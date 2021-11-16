package net.catenoid.watcher.upload.dto;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.kollus.json_data.BaseCommand;

public class ContentInfoDTO {
	/**
	 * container format
	 */
	@Expose
	@SerializedName("format")
	private String format = "Unkonwn";
	
	@Expose
	@SerializedName("duration")
	private String duration;
	
	/**
	 * Video Info
	 */
	@Expose
	@SerializedName("video.format")
	private String videoFormat;
	
	@Expose
	@SerializedName("video.duration")
	private String videoDuration;
	
	@Expose
	@SerializedName("video.codec")
	private String videoCodec;
	
	@Expose
	@SerializedName("video.bitrate")
	private String videoBitrate;
	
	@Expose
	@SerializedName("video.width")
	private String videoWidth;
	
	@Expose
	@SerializedName("video.height")
	private String videoHeight;
	
	@Expose
	@SerializedName("video.framerate")
	private String videoFrameRate;
	
	@Expose
	@SerializedName("video.ratio")
	private String videoRatio;
	
	@Expose
	@SerializedName("video.rotation")
	private String rotation;

	@Expose
	@SerializedName("video.scantype")
	private String scanType;

	/**
	 * Audio Info
	 */
	@Expose
	@SerializedName("audio.format")
	private String audioFormat;

	@Expose
	@SerializedName("audio.codec")
	private String audioCodec;
	
	@Expose
	@SerializedName("audio.bitrate")
	private String audioBitrate;
	
	@Expose
	@SerializedName("audio.sample.rate")
	private String audioSampleRate;
	
	@Expose
	@SerializedName("audio.duration")
	private String audioDuration;
	
	/**
	 * Image Info
	 */
	@Expose
	@SerializedName("image.format")
	private String imageFormat;
	
	@Expose
	@SerializedName("image.width")
	private String imageWidth;
	
	@Expose
	@SerializedName("image.height")
	private String imageHeight;
	
	
	
	
	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getVideoFormat() {
		return videoFormat;
	}

	public void setVideoFormat(String videoFormat) {
		this.videoFormat = videoFormat;
	}

	public String getVideoDuration() {
		return videoDuration;
	}

	public void setVideoDuration(String videoDuration) {
		this.videoDuration = videoDuration;
	}

	public String getVideoCodec() {
		return videoCodec;
	}

	public void setVideoCodec(String videoCodec) {
		this.videoCodec = videoCodec;
	}

	public String getVideoBitrate() {
		return videoBitrate;
	}

	public void setVideoBitrate(String videoBitrate) {
		this.videoBitrate = videoBitrate;
	}

	public String getVideoWidth() {
		return videoWidth;
	}

	public void setVideoWidth(String videoWidth) {
		this.videoWidth = videoWidth;
	}

	public String getVideoHeight() {
		return videoHeight;
	}

	public void setVideoHeight(String videoHeight) {
		this.videoHeight = videoHeight;
	}

	public String getVideoFrameRate() {
		return videoFrameRate;
	}

	public void setVideoFrameRate(String videoFrameRate) {
		this.videoFrameRate = videoFrameRate;
	}

	public String getVideoRatio() {
		return videoRatio;
	}

	public void setVideoRatio(String videoRatio) {
		this.videoRatio = videoRatio;
	}

	public String getScanType() {
		return scanType;
	}

	public void setScanType(String scanType) {
		this.scanType = scanType;
	}

	public String getAudioFormat() {
		return audioFormat;
	}

	public void setAudioFormat(String audioFormat) {
		this.audioFormat = audioFormat;
	}

	public String getAudioCodec() {
		return audioCodec;
	}

	public void setAudioCodec(String audioCodec) {
		this.audioCodec = audioCodec;
	}

	public String getAudioBitrate() {
		return audioBitrate;
	}

	public void setAudioBitrate(String audioBitrate) {
		this.audioBitrate = audioBitrate;
	}

	public String getAudioSampleRate() {
		return audioSampleRate;
	}

	public void setAudioSampleRate(String audioSampleRate) {
		this.audioSampleRate = audioSampleRate;
	}

	public String getAudioDuration() {
		return audioDuration;
	}

	public void setAudioDuration(String audioDuration) {
		this.audioDuration = audioDuration;
	}

	public String getImageFormat() {
		return imageFormat;
	}

	public void setImageFormat(String imageFormat) {
		this.imageFormat = imageFormat;
	}

	public String getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(String imageWidth) {
		this.imageWidth = imageWidth;
	}

	public String getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(String imageHeight) {
		this.imageHeight = imageHeight;
	}

	public String getRotation() {
		return rotation;
	}

	public void setRotation(String rotation) {
		if("".equals(rotation) || rotation==null) rotation="0";
		this.rotation = rotation;
	}

	
	public String toString() {
		final Gson gson = BaseCommand.gson(true);
		return gson.toJson(this);
	}
			
	public String toJSONEncodedString() throws UnsupportedEncodingException {
		String str = this.toString();
		return URLEncoder.encode(str, "UTF-8");
	}
}
