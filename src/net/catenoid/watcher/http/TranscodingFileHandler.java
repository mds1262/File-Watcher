package net.catenoid.watcher.http;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.utils.WatcherUtils;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.kollus.json_data.BaseCommand;

public class TranscodingFileHandler extends HandlerExt {

	private static Logger log = Logger.getLogger(TranscodingFileHandler.class);
		
	private interface PARAMS {
		public final String COMMAND = "cmd";
		public final String PROVIDER = "provider";
		public final String CONTENT_PATH = "content_path";
		public final String THUMBNAIL_FILES = "thumbnail_files";
	}
	
	@Override
	protected boolean runCommand(Map<String, Object> params, String result) {
		String command = (String) params.get(PARAMS.COMMAND);
		switch(command) {
		case "enable" :	return cmdEnable(params, result);
		case "disable" :	return cmdDisable(params, result);
		case "remove" : return cmdRemove(params, result);
		case "check" : return cmdCheck(params, result);
		}
		return false;
	}

	private boolean cmdRemove(Map<String, Object> params, String result) {

		long nowmil = System.currentTimeMillis();
		@SuppressWarnings("unused")
		String provider = (String) params.get(PARAMS.PROVIDER);
		String content_path = (String) params.get(PARAMS.CONTENT_PATH);		
		String str_thumbnail_files = (String) params.get(PARAMS.THUMBNAIL_FILES);		
		
		if(str_thumbnail_files != null && str_thumbnail_files.isEmpty() == false) {
			
			Gson gson = BaseCommand.gson(false);
			Type typeOfFiles = new TypeToken<String[]>() { }.getType();
			
			String[] thumbnail_files = null;
			try {
				thumbnail_files = gson.fromJson(str_thumbnail_files, typeOfFiles);
			} catch (JsonSyntaxException e) {
				log.error("thubmail_file json error: " + str_thumbnail_files);
				return false;
			}

			//	content_path = Utils.FilenameDefence(content_path);
			
			// delete thumbnail_files
			String thumbnail_dir = conf.getHttpserverConf().getThumbnailDir();

			// rm -rf /mnt/medianas/transcoding_file/catenodi/yyyymmdd/media_content_id/
			
			// file check
			// 파일이 존재하는지 사전 체크하는 부분 제거 요청(이주헌)으로 코드 주석 처리
			/*
			for(int i = 0; i < thumbnail_files.length(); i++) {
				String path = "";
				try {
					path = thumbnail_dir + thumbnail_files.getString(i);
					path = Utils.FilenameDefence(path);
					if(!isWindows) {
						// 명령 실행 후 file 객체로 존재확인
						if(isExist(path) == false) {
							log.error(LogAction.HTTP_LOG + "error not exist path :" + path);
							return false;
						}
					}
				} catch (JSONException e) {
					log.error("thubmail_file json error: " + str_thumbnail_files);
					return false;
				}
			}
			*/
			
//			for(int i = 0; i < thumbnail_files.length(); i++) {
			for(String thumbnail : thumbnail_files) {
				String path = thumbnail_dir + thumbnail;
				path = WatcherUtils.FilenameDefence(path);
				
				ArrayList<String> cmd = new ArrayList<String>();
				cmd.add("/bin/rm");
				cmd.add(path);
				
				log.trace(LogAction.COMMAND + cmd);
				
				LinuxExecute exec = new LinuxExecute(conf, cmd);
				exec.enableStdOutOff(true); // 파일 직접 확인하니까 output off
				try {
					exec.run();
				} catch (Exception e) {
					log.error(e.toString());
					return false;
				}
				
				if(isExist(path) == true) {
					log.error(LogAction.HTTP_LOG + "error file delete failed :" + path);
					return false;
				}
			}
		}

		content_path = WatcherUtils.FilenameDefence(content_path);
		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
		for(String dir : dirs) {
			
			String srcPath = makeContentPath(dir, content_path);
			String disablePath = makeDisableContentPath(dir, content_path);
			
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("/bin/rm");
			cmd.add(srcPath);
			cmd.add(disablePath);
			
			/**
			 * keti-transcoder와 같이 split된 파일에 대한 예외 처리 추가
			 */
			String srcPath2 = removeExt(srcPath);
			String disablePath2 = srcPath2 + "_";

			cmd.add(srcPath2);
			cmd.add(disablePath2);
			
			log.debug(LogAction.COMMAND + cmd.toString());
			
			if(!isDebug) {
				LinuxExecute exec = new LinuxExecute(conf, cmd);
				exec.enableStdOutOff(true); // 아래에서 파일 확인하기 때문에 stdout 출력 Off 시킴
				try {
					exec.run();
				} catch (Exception e) {
					log.error(e.toString());
					message = "provider path error";
					return false;
				}

				if(!isWindows) {
					// 명령 실행 후 file 객체로 존재확인
					if(isExist(srcPath) == true) {
						message = "exist : " + srcPath;
						log.error(LogAction.HTTP_LOG + "error exist path :" + srcPath);
						return false;
					}

					if(isExist(disablePath) == true) {
						message = "exist : " + disablePath;
						log.error(LogAction.HTTP_LOG + "error exist path :" + disablePath);
						return false;
					}
				}
			}
		}

		log.debug(LogAction.COMMAND + String.format("Transcoding files delete elimination time: %d ms", System.currentTimeMillis() - nowmil));
		
		return true;
	}
	
	private boolean cmdCheck(Map<String, Object> params, String result) {
		
		@SuppressWarnings("unused")
		String provider = (String) params.get(PARAMS.PROVIDER);
		String content_path = (String) params.get(PARAMS.CONTENT_PATH);

		boolean isExist = false;
		boolean isDisable = false;
		
		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
		for(String dir : dirs) {
			
			String srcPath = makeContentPath(dir, content_path);
			String disablePath = makeDisableContentPath(dir, content_path);

			if(!isWindows) {
				// 명령 실행 후 file 객체로 존재확인
				if(isExist(srcPath) == true) {
					isExist = true;
				}

				if(isExist(disablePath) == true) {
					isExist = true;
					isDisable = true;
				}
			}
		}
		
		class Result {
			public Result(boolean exist, boolean disable) {
				isExist = exist;
				isDisable = disable;
			}

			@Expose
			@SerializedName("exist")
			private boolean isExist;
			
			@Expose
			@SerializedName("disable")
			private boolean isDisable;
			
			public String toString() {
				final Gson gson = BaseCommand.gson(false);
				return gson.toJson(this);
			}
		}
		
		Result r = new Result(isExist, isDisable);
		message = r.toString();
		
		return true;
	}

	private boolean cmdEnable(Map<String, Object> params, String result) {
		
		@SuppressWarnings("unused")
		String provider = (String) params.get(PARAMS.PROVIDER);
		String content_path = (String) params.get(PARAMS.CONTENT_PATH);
		
		// transcoding_fils
		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
		for(String dir : dirs) {
			
			String srcPath = makeContentPath(dir, content_path);
			String disablePath = makeDisableContentPath(dir, content_path);
			
			/**
			 * keti-transcoder와 같이 split된 파일에 대한 예외 처리 추가
			 * 2016-03-29 예외 코드로 문제가 발생하여 원복시킴
			 */
//			if(isExist(disablePath) == false) {
//				srcPath = removeExt(disablePath);
//				disablePath = srcPath + "_";
//			}
			
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("/bin/mv");
			cmd.add(disablePath);
			cmd.add(srcPath);
			
			log.trace(LogAction.COMMAND + cmd.toString());
			
			if(!isDebug) {
				LinuxExecute exec = new LinuxExecute(conf, cmd);
				try {
					exec.run();
				} catch (Exception e) {
					log.error(e.toString());
					message = "provider path error";
					return false;
				}
				
				// 명령 실행 후 file 객체로 존재확인
				if(isExist(srcPath) == false) {
					log.error(LogAction.HTTP_LOG + "error not exist path :" + srcPath);
					return false;
				}
				if(isExist(disablePath) == true) {
					log.error(LogAction.HTTP_LOG + "error exist path :" + disablePath);
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param params
	 * @param result
	 * @return
	 */
	private boolean cmdDisable(Map<String, Object> params, String result) {
		
		/**
		 * 2016-03-29
		 * 웹개발파트의 요청으로 disable은 아무런 작업도 하지 않고 성공시키도록 한다.
		 */
		return true;
		
//		@SuppressWarnings("unused")
//		String provider = (String) params.get(PARAMS.PROVIDER);
//		String content_path = (String) params.get(PARAMS.CONTENT_PATH);
//
//		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
//		for(String dir : dirs) {
//			
//			String srcPath = makeContentPath(dir, content_path);
//			String disablePath = makeDisableContentPath(dir, content_path);
//			
//			/**
//			 * keti-transcoder와 같이 split된 파일에 대한 예외 처리 추가
//			 */
//			if(isExist(srcPath) == false) {
//				srcPath = removeExt(srcPath);
//				disablePath = srcPath + "_";
//			}
//			
//			ArrayList<String> cmd = new ArrayList<String>();
//			cmd.add("/bin/mv");
//			cmd.add(srcPath);
//			cmd.add(disablePath);
//			
//			log.debug(LogAction.COMMAND + cmd.toString());
//			
//			if(!isDebug) {			
//				LinuxExecute exec = new LinuxExecute(conf, cmd);
//				try {
//					exec.run();
//				} catch (Exception e) {
//					log.error(e.toString());
//					message = "provider path error";
//					return false;
//				}
//				
//				if(!isWindows) {
//					// 명령 실행 후 file 객체로 존재확인
//					if(isExist(srcPath) == true) {
//						log.error(LogAction.HTTP_LOG + "error exist path :" + srcPath);
//						return false;
//					}
//					if(isExist(disablePath) == false) {
//						log.error(LogAction.HTTP_LOG + "error exist path :" + disablePath);
//						return false;
//					}
//				}
//			}
//		}
	}
	
	@Override
	protected boolean checkParams(Map<String, Object> params) {
		if(checkParam(params, PARAMS.COMMAND) == false) return false;
		if(checkParam(params, PARAMS.PROVIDER) == false) return false;
		if(checkParam(params, PARAMS.CONTENT_PATH) == false) return false;
//		if(checkParam(params, PARAMS.THUMBNAIL_FILES) == false) return false;
		return true;
	}

	@SuppressWarnings("unused")
	private String makeResult(int error, String message) {
		return "";
	}

}
