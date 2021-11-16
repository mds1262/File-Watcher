package net.catenoid.watcher.upload.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.utils.WatcherUtils;

import org.apache.log4j.Logger;

/**
 * 
 * @author firewolf
 * 
 * 트랜스코더에서 사용중인 새로90 명령 샘플
 * ffmpeg -i /288_20141208-zkgn5ohg.MOV -vf transpose=1 -an -r 1 -s 60x108 0_0_%04d.jpg
 */
public class SnapCreator {
	
	private static Logger log = Logger.getLogger(SnapCreator.class);
	private static final String TEMP_SNAP_FILE_NAME = "CONTENT_CHECK_TEMP_SNAP";
	public static final String TEMP_SNAP_FILE_EXT = "JPEG";

	private FileItemDTO fileitem = null;
	private Config conf = null;	
	private boolean snap_create_simulator = false;
	private String jobName;
	
	public SnapCreator(Config config, String jobName, FileItemDTO file)
	{
		this.conf = config;
		this.fileitem = file;
		this.jobName = jobName;
	}

	public void setSimulator(boolean isSimulator) {
		this.snap_create_simulator = isSimulator;
	}
	
	/**
	 * 임시로 생성된 파일을 자동으로 삭제한다.
	 * @return
	 */
	private boolean isSimulator() {
		return this.snap_create_simulator;
	}

	class InputHandler extends Thread {

		private static final int BUFFER_SIZE = 1024;
		private InputStream input_;
		private int mLines = 0;

	    InputHandler(InputStream input, String name) {
	        super(name);
	        input_ = input;
	    }

	    public void run() {
	        
	        BufferedReader reader = null;
			try
			{
				reader = new BufferedReader(new InputStreamReader(input_), BUFFER_SIZE);
				String line;
				
				while ((line = reader.readLine()) != null)
				{
					onNewline(line);
					mLines ++;
					
					/**
					 * 혹시 MAX_VALUE 보다 크다면 0으로 초기화
					 */
					if(mLines == Integer.MAX_VALUE) mLines = 0;
				}
				
			}
			catch (IOException e)
			{
			}
			finally
			{
				if (reader != null)
					try { reader.close(); } catch (IOException e) {}
					
				stopCatter();
			}
	    }

		private void stopCatter()
		{
			if(childProcess != null)
			{
				childProcess.destroy();
			}
		}

		private void onNewline(String line)
		{
			//log.debug(this.getName() + " - " + line);
		}
		
	}
	
	private Process childProcess = null;

	/**
	 * 썸네일 획득을 위해 FFMPEG 프로세스를 실행한다.<br>
	 * mediainfo,duration을 획득하지 못하면 poster를 생성하지 않는다.
	 * @return
	 * @throws Exception
	 */
	public boolean  run(int type) {
		
//		log.debug("SnapCreator - run");
		
		long _start = System.currentTimeMillis();
		
		String tempSnapPath = String.format("%s/%s/%s_%d.%s", conf.getSnap().getSnapTempDir(), jobName, TEMP_SNAP_FILE_NAME, _start, TEMP_SNAP_FILE_EXT);

		// videoDuration이 null이라면 general duration 사용.
		float _fsecond;
		if(fileitem.getMediaInfo().getVideoDuration() == null){
			_fsecond = Float.parseFloat(fileitem.getMediaInfo().getDuration()) / 1000;
		} else {
			_fsecond = Float.parseFloat(fileitem.getMediaInfo().getVideoDuration()) / 1000;
		}
		int _isecond = (int) _fsecond;
		int second = (int) _fsecond;
		log.debug(String.format("[INFO-3] _fsecond = " + _fsecond));
		/**
		 * 파일의 duration이 0(zero)이면 비정상 파일로 인식하고 처리하지 않는다.
		 */
		if(second == 0) {
			return false;
		}

		Poster custom_info = null;		
		if(fileitem.getPoster() != null && fileitem.getPoster().getPosition() != -1) {
			custom_info = fileitem.getPoster();
		} else {
			custom_info = conf.getSnap().get_custom_snapshot(fileitem.getContentProviderKey());
		}
		
		/**
		 * conf에 지정된 시간 위치의 썸네일을 획득한다.
		 * 컨텐츠의 길이가 snapshot 추출 위치보다 작은경우 중간 위치에서 추출한다.
		 */
		if(custom_info != null) {
			second = custom_info.getPosition();
		} else {
			custom_info = new Poster(conf.getSnap().getSnapSecond(), conf.getSnap().getWidth(), conf.getSnap().getHeight());
			second = conf.getSnap().getSnapSecond();
		}
		
		if(second >= _isecond) {
			if(_isecond==1) _isecond=2;
			second = _isecond/2; 
		}
		String startOffset = WatcherUtils.secondsToString(second);	

		if(isSimulator() == false) {
			/**
			 * 임시저장 공간이 아닌 실제 저장 위치를 지정하도록 한다.			
			 */
			//String temp = String.format("%s%s.jpg", fileItem.snapshot_path, fileItem.key);
			String temp = String.format("%s", fileitem.getSnapshotPath());
			
			//  /mnt/medianas/snapshot//daekyo-warac/20141119-5odvo2mu.jpg -->  /mnt/medianas/snapshot/daekyo-warac/20141119-5odvo2mu.jpg
			tempSnapPath = String.format("%s%s", conf.getSnap().getSnapDir(), temp);
			//log.debug("SNAP PATH: " + tempSnapPath);
			
		} else {
			/*
			if(fileItem.mediaInfo.videoCodec.compareToIgnoreCase("wmv3") == 0) {
				if(Utils.isEmpty(conf.getWmvThumbDir())) {
					return true;	
				}				
			}
			*/			
		}		
		
		ArrayList<String> array = new ArrayList<String>();
		array.add(conf.get_ffmpeg_dir());
		array.add("-report");
		array.add("-y");
		array.add("-ss");
		array.add(startOffset);
		array.add("-i");
		array.add(fileitem.getPhysicalPath());
		array.add("-vf");
		/**
		 * ffmpeg 버전에 따라서 auto rotation을 지원하지 않는 경우가 있음.
		 */
//		if (Float.valueOf(fileitem.mediaInfo.getRotation()) > 0) {
//			array.add(this.rotationToString());
//		} else {
//			array.add("scale=" + this.scaleString(custom_info));
//		}
		array.add("scale=" + this.scaleStringExt(custom_info));
		
		array.add("-vframes");
		array.add("1");
		array.add(tempSnapPath);

//		if (type != 0) {
//			array.clear();
//
//			array.add(conf.get_ffmpeg_dir());
//			array.add("-y");
//			array.add("-ss");
//			array.add(startOffset);
//			array.add("-i");
//			array.add(fileitem.get_physical_path());
//			array.add("-vframes");
//			array.add("1");
//			array.add("-s");
//			array.add(this.resizeToString());
//			array.add(tempSnapPath);
//			
//			String[] cmdArray = new String[array.size()];
//			array.toArray(cmdArray);
//			log.error("check cmd:"+Arrays.toString(cmdArray));
//		}
		File file = new File(tempSnapPath);
		if(file.exists()) {
			file.delete();
		}
		File Folder = new File("/home/kollus/MediaWatcher2/history");
		if (!Folder.exists()) {
			try{
			    Folder.mkdir(); //폴더 생성합니다.
		    } 
		    catch(Exception e){
			}        
	    }
		
		{
			File snapDir = file.getParentFile();
			snapDir.mkdirs();
			WatcherUtils.chmod777(snapDir.getAbsolutePath());
		}
		
		try
		{			
			//childProcess = Runtime.getRuntime().exec(cmd);
			String[] cmdArray = new String[array.size()];
			array.toArray(cmdArray);
			log.debug("snapshot cmd:"+Arrays.toString(cmdArray));
			ProcessBuilder pb = new ProcessBuilder(cmdArray);
			//pb.redirectErrorStream(true);
			
			Map<String, String> env = pb.environment();
			env.put("FFREPORT", String.format("file=/home/kollus/MediaWatcher2/history/%%p-%s.log:level=32", new SimpleDateFormat("YYYYMMdd-HHmmssSSS").format(System.currentTimeMillis())));
			
			childProcess = pb.start();
	        InputHandler inputHandler = new InputHandler(childProcess.getInputStream(), "Output Stream");
	        inputHandler.start();
	        InputHandler errorHandler = new InputHandler(childProcess.getErrorStream(), "Error Stream");
	        errorHandler.start();
//	        String line1 = null;
//	        BufferedReader error = new BufferedReader(new InputStreamReader(
//	        	childProcess.getErrorStream()));
//			while ((line1 = error.readLine()) != null) {
//				log.debug(line1);
//			}
	        childProcess.waitFor();

			@SuppressWarnings("unused")
			
			int exitCode = childProcess.exitValue();
			log.debug("exitCode: "+ exitCode);
			if (exitCode != 0) {
				log.debug("exit code is not 0 [ "+ exitCode + "]");
			}
			@SuppressWarnings("unused")
			long _end = System.currentTimeMillis();
			
			destroy();
		} catch (Exception e) {
			e.printStackTrace();
			
			long _end = System.currentTimeMillis();
			log.debug(String.format("filename: %s , dur[%s]", fileitem.getPhysicalPath(), WatcherUtils.millisToDHMS(_end - _start)));
			
			destroy();
		}
		
		if(file.exists() && file.isFile() && file.length() > 0) {
			if(isSimulator()) {
				file.delete();
			}					

			destroy();
			return true;
		}

		if(isSimulator()) {
			file.delete();
		}					
		
		return false;
	}	
	

	private void destroy()
	{
		if(childProcess != null)
		{
			childProcess.destroy();
		}
	}
	
	
	/*
	 *  원본 동영상 사이즈 셋팅
	 * 
	 */
	
    public String scaleString(Poster custom_info){
    	int org_media_width=Integer.valueOf(fileitem.getMediaInfo().getVideoWidth());
    	int org_media_height=Integer.valueOf(fileitem.getMediaInfo().getVideoHeight());
    	
//    	if(Float.valueOf(fileitem.mediaInfo.getRotation()) == 90 ||
//    			Float.valueOf(fileitem.mediaInfo.getRotation()) == 270) {
//    		org_media_width=Integer.valueOf(fileitem.mediaInfo.videoHeight);
//        	org_media_height=Integer.valueOf(fileitem.mediaInfo.videoWidth);
//    	}
    	
    	int output_height = 0;
    	if(custom_info != null) {
        	output_height =(org_media_width !=0 && org_media_height !=0 )
        			? Math.round(org_media_height * custom_info.getWidth() / org_media_width) 
    				: custom_info.getHeight();
        	return String.format("%d:%d",custom_info.getWidth(), output_height);
    	} else {
        	output_height =(org_media_width !=0 && org_media_height !=0 )
        			? Math.round(org_media_height * conf.getSnap().getWidth() / org_media_width) 
    				: conf.getSnap().getHeight();
        	return String.format("%d:%d",conf.getSnap().getWidth(), output_height);
    	}
    }
    
    public String scaleStringExt(Poster custom_info){
    	
    	int org_media_width=Integer.valueOf(fileitem.getMediaInfo().getVideoWidth());
    	int org_media_height=Integer.valueOf(fileitem.getMediaInfo().getVideoHeight());
		
    	int transpose=0;    	
    	float rotation=Float.valueOf(fileitem.getMediaInfo().getRotation());
    	if(rotation==90) transpose=1;
    	if(rotation==270) transpose=2;
    	if(rotation==180) transpose=3;

    	int posterWidth = custom_info.getWidth();
    	int posterHeight = custom_info.getHeight();
    	String transposeStr = "";
    	if(conf.isFfmpegAutoRotation() == false) {
	    	switch(transpose) {
	    	case 0:
	    		break;
	    	case 1:
	    	case 2:
	    		transposeStr = String.format(",transpose=%d", transpose);
	    		posterWidth = custom_info.getHeight();
	        	posterHeight = custom_info.getWidth();
	    		break;
	    	case 3:
	    		transposeStr = String.format(",hflip,vflip");
	    		break;
	    	}
    	} else {
			// 원본 영상의 회전 값(90도, 270도)에 따라 영상 width/height 값을 swap시킨다.
			if(rotation == 90 || rotation == 270) {
				int tmp = org_media_width;
				org_media_width = org_media_height;
				org_media_height = tmp;
			}
		}
    	
    	float scale_width = (float)org_media_width / (float) posterWidth;
    	int height = (int)((float) org_media_height / scale_width);
    	int width = posterWidth;
    	
    	if(height > posterHeight) {
    		float scale_height = (float)org_media_height / (float)posterHeight;
    		height = posterHeight;
    		width = (int)((float)org_media_width / scale_height);
    	}

    	return String.format("%d:%d%s", width, height, transposeStr);
    }   
    
	public String resizeToString(){
		int org_media_width=Integer.valueOf(fileitem.getMediaInfo().getVideoWidth());
    	int org_media_height=Integer.valueOf(fileitem.getMediaInfo().getVideoHeight());
    	int output_height =(org_media_width !=0 && org_media_height !=0 )? Math.round(org_media_height * conf.getSnap().getWidth() / org_media_width) 
				: conf.getSnap().getHeight();
		
		return String.format("%d*%d",conf.getSnap().getWidth(), output_height);
	}
	
	public String rotationToString(){
		
		int org_media_width=Integer.valueOf(fileitem.getMediaInfo().getVideoWidth());
    	int org_media_height=Integer.valueOf(fileitem.getMediaInfo().getVideoHeight());
    	
    	if(conf.isFfmpegAutoRotation() == false) {
    		if(Float.valueOf(fileitem.getMediaInfo().getRotation()) == 90 ||
    				Float.valueOf(fileitem.getMediaInfo().getRotation()) == 270) {
    			org_media_width=Integer.valueOf(fileitem.getMediaInfo().getVideoHeight());
    			org_media_height=Integer.valueOf(fileitem.getMediaInfo().getVideoWidth());
    		}
    	} 

    	int conf_width=conf.getSnap().getWidth();
    	int transpose=0;
    	
    	float rotation=Float.valueOf(fileitem.getMediaInfo().getRotation());
    	if(rotation==90) transpose=1;
    	if(rotation==180) transpose=-1;
    	if(rotation==270) transpose=2;
    	
    	int output_height =(org_media_width !=0 && org_media_height !=0 )? Math.round(org_media_height * conf.getSnap().getWidth() / org_media_width) 
				: conf.getSnap().getHeight();
   	
    	
    	int scale_width=(conf.getSnap().getWidth()-(conf.getSnap().getWidth()-output_height));
    	int pad_width_diff=Math.round((conf.getSnap().getWidth()-output_height)/2);
		//-vf "scale=360:360,pad=360:640:0:70,transpose=1"
    	if(transpose == 1 || transpose == 2)
    	   return String.format("scale=%d:%d,hflip,vflip",conf.getSnap().getWidth(), output_height);
    	else
    		return String.format("scale=%d:%d,pad=%d:%d:0:%d,transpose=%d",output_height,scale_width,output_height+1,conf_width,pad_width_diff,transpose);
		
	}
	
}
