package net.catenoid.watcher.http;

import java.util.ArrayList;
import java.util.Map;

import net.catenoid.watcher.LogAction;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.kollus.json_data.BaseCommand;
import com.kollus.utils.myUtils;

public class MediaContentHandler extends HandlerExt {

	private static Logger log = Logger.getLogger(MediaContentHandler.class);
		
	private interface PARAMS {
		public final String COMMAND = "cmd";
		public final String PROVIDER = "provider";
		public final String SNAPSHOT = "snapshot_file";
		public final String THUMBNAIL_PATH = "thumbnail_path";
		public final String MEDIA_CONTENT_ID = "media_content_id";
		public final String MEDIA_PATH = "media_path";
		public final String ORIGIN_PATH = "origin_path";
		public final String CONTENT_PATH = "content_path";
	}

	@Override
	protected boolean runCommand(Map<String, Object> params, String result) {
		String command = (String) params.get(PARAMS.COMMAND);
		switch(command) {
		case "remove" : return cmdRemove(params, result);
		case "check" : return cmdCheck(params, result);
		}
		return false;
	}

	private boolean cmdRemove(Map<String, Object> params, String result) {

		long nowmil = System.currentTimeMillis();

		String provider = (String) params.get(PARAMS.PROVIDER);		
		String backup_path = (String) params.get(PARAMS.ORIGIN_PATH);
		String snapshot = (String) params.get(PARAMS.SNAPSHOT);
		String thumbnail_path = (String) params.get(PARAMS.THUMBNAIL_PATH);
		String media_content_id = (String) params.get(PARAMS.MEDIA_CONTENT_ID);
		String media_path = (String) params.get(PARAMS.MEDIA_PATH);
//		String content_path = (String) params.get(PARAMS.CONTENT_PATH);
		
		/*
		[2014-11-27 01:20:24][TRACE](MediaContentHandler.java:57) - provider : tropian
		[2014-11-27 01:20:24][TRACE](MediaContentHandler.java:58) - backup_path : /tropian/20141127/11048/11048_20141127-f3m62hys.wmv
		[2014-11-27 01:20:24][TRACE](MediaContentHandler.java:59) - snapshot : /tropian/20141127/20141127-f3m62hys.jpg
		[2014-11-27 01:20:24][TRACE](MediaContentHandler.java:60) - thumbnail_path : /tropian/20141127/11048
		[2014-11-27 01:20:24][TRACE](MediaContentHandler.java:61) - media_content_id : 11048
		[2014-11-27 01:20:24][TRACE](MediaContentHandler.java:62) - media_path : /tropian/20141127/11048
		[2014-11-27 01:20:24][TRACE](MediaContentHandler.java:63) - working_path : /tropian/20141127-f3m62hys.wmv
		[2014-11-27 01:20:24][DEBUG](MediaContentHandler.java:266) - [SH_COMMAND][/bin/rm, -rf, /mnt/medianas/original/tropian/20141127/11048]
		[2014-11-27 01:20:24][DEBUG](MediaContentHandler.java:215) - [SH_COMMAND][/bin/sh, -c, /bin/rm -rf /mnt/medianas/thumbnail/tropian/20141127/11048*]
		[2014-11-27 01:20:24][DEBUG](MediaContentHandler.java:215) - [SH_COMMAND][/bin/sh, -c, /bin/rm -rf /mnt/medianas/snapshot/tropian/20141127/20141127-f3m62hys.jpg]
		[2014-11-27 01:20:24][DEBUG](MediaContentHandler.java:215) - [SH_COMMAND][/bin/sh, -c, /bin/rm -rf /mnt/medianas/working/tropian/20141127-f3m62hys.wmv]
		[2014-11-27 01:20:24][DEBUG](MediaContentHandler.java:122) - [SH_COMMAND][/bin/rm, -rf, /mnt/medianas/transcoding_file/tropian/20141127/11048]
		[2014-11-27 01:20:24][DEBUG](WorkingFileHandler.java:55) - [SH_COMMAND][/bin/rm, -rf, /mnt/medianas/working/tropian/20141127-f3m62hys.wmv]
		 */
		log.trace("provider : " + provider);
		log.trace("backup_path : " + backup_path);
		log.trace("snapshot : " + snapshot);
		log.trace("thumbnail_path : " + thumbnail_path);
		log.trace("media_content_id : " + media_content_id);
		log.trace("media_path : " + media_path);
//		log.trace("content_path : " + content_path);
		
		/**
		 * backup path가 6자 이하일수는 없을 것이기 때문에 사용함 
		 */
		if(backup_path != null && backup_path.length() > 6) {
			if(deleteBackup(provider, backup_path) == false) {
				log.error("deleteBackup: " + backup_path);
				return false;
			}
		}

		// delete thumbnail
		if (thumbnail_path != null 
				&& thumbnail_path.length() > 6 
				&& media_content_id != null 
				&& media_content_id.length() > 0
				&& media_content_id.trim().length() > 0) {
			String dir = conf.getHttpserverConf().getThumbnailDir();
			//String filename = String.format("%s/%s", thumbnail_path, media_content_id);
			String thumbnailDir = thumbnail_path;// 디렉토리 삭제 --섬네일 디렉토리 /minsoo/20131223/{컨텐츠ID}
			if(deleteFile(dir, thumbnailDir) == false) {
				log.error("Delete Thumbnail Dir: " + thumbnailDir);
				return false;
			}
		}
		
		// delete snapshot 
		if(snapshot != null && snapshot.length() > 6) {
			String dir = conf.getHttpserverConf().getSnapshotDir();
			String filename = snapshot;
			if(deleteFile(dir, filename) == false) {
				log.error("deleteSnapshot: " + filename);
				return false;
			}
		}

		// 2014-11-27 : CMS(윤영진C) 요청으로 working 삭제 제거
		// delete working file
//		if(content_path != null && content_path.length() > 0) {
//			String dir = conf.getHttpserverConf().getWorkingDir();
//			if(deleteFile(dir, content_path, false) == false) {
//				log.error("deleteWorking: " + dir + content_path);
//				return false;
//			}
//		}

		// delete media_content
		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
		for(String dir : dirs) {
			
			// rm -rf /mnt/medianas/transcoding_file/catenodi/yyyymmdd/media_content_id/
			String path = makeContentPath(dir, media_path);
		
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("/bin/rm");
			cmd.add("-rf");
			cmd.add(path);
			
			log.debug(LogAction.COMMAND + cmd);
			
			LinuxExecute exec = new LinuxExecute(conf, cmd);
			try {
				exec.run();
			} catch (Exception e) {
				e.printStackTrace();
				log.error("deleteContent: " + path);
				return false;
			}
			
			if(!isWindows) {
				// 명령 실행 후 file 객체로 존재확인
				if(isExist(path) == true) {
					log.error(LogAction.HTTP_LOG + "error exist path :" + path);
					return false;
				}
			}
		}


		log.debug(LogAction.COMMAND + String.format("Media contents files delete elimination time: %d ms", System.currentTimeMillis() - nowmil));
		return true;
	}
	
	private boolean cmdCheck(Map<String, Object> params, String result) {
		
		String provider = (String) params.get(PARAMS.PROVIDER);		
		String origin_path = (String) params.get(PARAMS.ORIGIN_PATH);
		String snapshot = (String) params.get(PARAMS.SNAPSHOT);
		String thumbnail_path = (String) params.get(PARAMS.THUMBNAIL_PATH);
		String media_content_id = (String) params.get(PARAMS.MEDIA_CONTENT_ID);
		String media_path = (String) params.get(PARAMS.MEDIA_PATH);
		
		boolean isExist = false;
		boolean isBackup = false;
		boolean isSnapshot = false;
		
		/**
		 * origin_path 가 6자 이하일수는 없을 것이기 때문에 사용함 
		 */
		if(origin_path != null && origin_path.length() > 6) {
			if(check_origin_file(provider, origin_path) == false) {
				isBackup = true;
			}
		}
	
		if(snapshot != null && snapshot.length() > 6) {
			String dir = conf.getHttpserverConf().getSnapshotDir();
			String filename = snapshot;
			if(checkFile(dir, filename) == true) {
				isSnapshot = true;
			}
		}

		// delete media_content
		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
		for(String dir : dirs) {
			
			// rm -rf /mnt/medianas/transcoding_file/catenodi/yyyymmdd/media_content_id/
			String path = makeContentPath(dir, media_path);
				
			if(!isWindows) {
				// 명령 실행 후 file 객체로 존재확인
				if(isExist(path) == true) {
					isExist = true;
				}
			}
		}
		
		class Api_Result {
			public Api_Result(boolean isExist, boolean isBackup, boolean isSnapshot) {
				exist = isExist;
				backup = isBackup;
				snapshot = isSnapshot;
			}

			@Expose
			public boolean exist = false;
			
			@Expose
			public boolean backup = false;
			
			@Expose
			public boolean snapshot = false;
			
			public String toString() {
				final Gson gson = BaseCommand.gson(false);
				return gson.toJson(this);
			}
		}
		
		Api_Result checkResult = new Api_Result(isExist, isBackup, isSnapshot);
		message = checkResult.toString();
		
		
		return true;
	}
	
	private boolean deleteFile(String dir, String filename) {
		
		String path = String.format("%s%s", dir, filename);
		path = myUtils.FilenameDefence(path);
		
		ArrayList<String> cmd = new ArrayList<String>();
//		cmd.add("/bin/sh");
//		cmd.add("-c");
//		cmd.add("/bin/rm -rf " + path);

		cmd.add("/bin/rm");
		cmd.add("-rf");
		cmd.add(path);

		log.debug(LogAction.COMMAND + cmd.toString());

		if(!isDebug) {
			LinuxExecute exec = new LinuxExecute(conf, cmd);
			try {
				exec.run();
			} catch (Exception e) {
				e.printStackTrace();
				message = "provider path error";
				return false;
			}

			if(!isWindows) {
				// 명령 실행 후 file 객체로 존재확인
				if(isExist(path) == true) {
					log.error(LogAction.HTTP_LOG + "error exist path :" + path);
					return false;
				}		
			}
		}

		return true;
	}
	
	private boolean checkFile(String dir, String filename) {
		
		String path = String.format("%s%s", dir, filename);

		if(!isWindows) {
			// 명령 실행 후 file 객체로 존재확인
			if(isExist(path) == true) {
				return true;
			}		
		}

		return false;
	}
	
	private boolean deleteBackup(String provider, String backupFile) {
		
		int existCount = 0;
		ArrayList<String> backupDir = conf.getHttpserverConf().getBackupDir();
		for(String dir : backupDir) {

			String filename = makeBackupContentFile(dir, backupFile);			
			ArrayList<String> cmd_file_remove = new ArrayList<String>();
			cmd_file_remove.add("/bin/rm");
			cmd_file_remove.add(filename);
			
			log.debug(LogAction.COMMAND + cmd_file_remove.toString());
			
			if(!isDebug) {
				LinuxExecute exec = new LinuxExecute(conf, cmd_file_remove);
				exec.enableStdOutOff(true); // 파일 확인을 직접하니까 Off
				try {
					exec.run();
				} catch (Exception e) {
					e.printStackTrace();
					message = "provider path error";
					return false;
				}

				if(!isWindows) {
					// 명령 실행 후 file 객체로 존재확인
					if(isExist(filename) == true) {
						log.error(LogAction.HTTP_LOG + "error exist path :" + filename);
						existCount++;
					}	
				}
			}
			
			String path = makeBackupContentPath(dir, backupFile);			
			ArrayList<String> cmd_path_remove = new ArrayList<String>();
			cmd_path_remove.add("/bin/rmdir");
			cmd_path_remove.add(path);
			
			log.debug(LogAction.COMMAND + cmd_path_remove.toString());
			
			if(!isDebug) {
				LinuxExecute exec = new LinuxExecute(conf, cmd_path_remove);
				exec.enableStdOutOff(true); // 파일 확인을 직접하니까 Off
				try {
					exec.run();
				} catch (Exception e) {
					e.printStackTrace();
					message = "provider path error";
					return false;
				}

				if(!isWindows) {
					// 명령 실행 후 file 객체로 존재확인
					if(isExist(path) == true) {
						log.error(LogAction.HTTP_LOG + "error exist path :" + path);
						existCount++;
					}	
				}
			}
		}
		
		return existCount > 0 ? false : true;
	}
	
	private boolean check_origin_file(String provider, String backupFile) {		
		int existCount = 0;
		ArrayList<String> backupDir = conf.getHttpserverConf().getBackupDir();
		for(String dir : backupDir) {
			String path = makeBackupContentFile(dir, backupFile);			
			if(!isWindows) {
				// 명령 실행 후 file 객체로 존재확인
				if(isExist(path) == true) {
					existCount++;
				}	
			}
		}		
		return existCount > 0 ? false : true;
	}
	
	@Override
	protected boolean checkParams(Map<String, Object> params) {
		if(checkParam(params, PARAMS.COMMAND) == false) return false;
		if(checkParam(params, PARAMS.PROVIDER) == false) return false;
		if(checkParam(params, PARAMS.MEDIA_PATH) == false) return false;
		
		String thumbnail_path = (String) params.get(PARAMS.THUMBNAIL_PATH);
		String media_content_id = (String) params.get(PARAMS.MEDIA_CONTENT_ID);

		if(thumbnail_path != null) {
			if(thumbnail_path.length() > 6) {
				if(media_content_id != null && media_content_id.length() > 0 && media_content_id.trim().length() > 0) {

				} else {
					message = "params empty :" + PARAMS.MEDIA_CONTENT_ID;
					return false;
				}
			} else {
				message = "params empty :" + PARAMS.THUMBNAIL_PATH;
				return false;				
			}
		}
		return true;
	}
	
	private String makeResult(int error, String message) {
		return "";
	}

}
