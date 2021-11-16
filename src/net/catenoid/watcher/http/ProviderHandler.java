package net.catenoid.watcher.http;

import java.util.ArrayList;
import java.util.Map;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.utils.WatcherUtils;

import org.apache.log4j.Logger;

public class ProviderHandler extends HandlerExt {

	private static Logger log = Logger.getLogger(ProviderHandler.class);
	
	private interface PARAMS {
		public final String COMMAND = "cmd";
		public final String PROVIDER = "provider";
	}
	
	@Override
	protected boolean runCommand(Map<String, Object> params, String result) {
		
		String command = (String) params.get(PARAMS.COMMAND);		
		result = command;
		
		switch(command) {
		case "register" :	return cmdRegister(params, result);
		case "unregister" : return cmdUnregister(params, result);
		case "enable" : return cmdEnable(params, result);
		case "disable" : return cmdDisable(params, result);
		}
		return false;
	}

	private boolean cmdDisable(Map<String, Object> params, String result) {
		
		/**
		 * 2016-03-29 웹개발 파트의 요청으로 disable은 아무런 작업을 하지 않고 무조건 성공하도록 한다.
		 */
		return true;
		
//		String provider = (String) params.get(PARAMS.PROVIDER);		
//		provider = myUtils.FilenameDefence(provider);
//		
//		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
//		for(String dir : dirs) {
//			
//			String path = makeProviderPath(dir, provider);
//			String disablePath = makeProviderDisablePath(dir, provider);
//			
//			ArrayList<String> cmd = new ArrayList<String>();
//			cmd.add("/bin/mv");
//			cmd.add(path);
//			cmd.add(disablePath);
//			
//			log.debug(LogAction.COMMAND + cmd.toString());
//			
//			LinuxExecute exec = new LinuxExecute(conf, cmd);
//			try {
//				exec.run();
//			} catch (Exception e) {
//				log.error(e.toString());
//				message = "provider path error";
//				return false;
//			}
//			
//			if(!isWindows) {
//				// 명령 실행 후 file 객체로 존재확인
//				if(isExist(path) == true) {
//					message = "exist path error:" + disablePath;
//					log.error(LogAction.HTTP_LOG + "error exist path :" + disablePath);
//					return false;
//				}
//			}
//		}
//		
//		return true;
	}

	private boolean cmdEnable(Map<String, Object> params, String result) {
		String provider = (String) params.get(PARAMS.PROVIDER);
		
		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
		for(String dir : dirs) {
			
			String path = makeProviderPath(dir, provider);
			String disablePath = makeProviderDisablePath(dir, provider);
			
			// ln -s /mnt/medianas/transcoding_file/[provider] /mnt/medianas/transcoding_file/[provider]/[provider]_[revision]
			
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("/bin/mv");
			cmd.add(disablePath);
			cmd.add(path);
			
			log.trace(LogAction.COMMAND + cmd.toString());
			
			LinuxExecute exec = new LinuxExecute(conf, cmd);
			try {
				exec.run();
			} catch (Exception e) {
				log.error(e.toString());
				return false;
			}
			
			if(!isWindows) {
				// 명령 실행 후 file 객체로 존재확인
				if(isExist(path) == false) {
					message = "not exist path error:" + path;
					log.error(LogAction.HTTP_LOG + "error exist path :" + path);
					return false;
				}
			}
		}
		
		return true;
	}

	private boolean cmdUnregister(Map<String, Object> params, String result) {
		
		String provider = (String) params.get(PARAMS.PROVIDER);
		
		if(cmdDisable(params, result) == false) {
			return false;
		}

		/**
		 * FTP 폴더 삭제
		 */
//		ArrayList<String> array = conf.getHttpserverConf().getFtpDir();
//		for(int i = 0; i < array.size(); i++) {
//			String ftpRoot = array.get(i);
//			String path = makeFtpPath(ftpRoot, provider);
//			
//			ArrayList<String> cmd = new ArrayList<String>();
//			cmd.add("/bin/rm");
//			cmd.add("-rf");
//			cmd.add(path);
//			
//			log.debug(LogAction.COMMAND + cmd.toString());
//			
//			LinuxExecute exec = new LinuxExecute(conf, cmd);
//			try {
//				exec.run();
//			} catch (Exception e) {
//				log.error(e.toString());
//				message = "provider path error";
//				return false;
//			}
//			
//			if(!isWindows) {
//				// 명령 실행 후 file 객체로 존재확인
//				if(isExist(path) == true) {
//					message = "not exist path error:" + path;
//					log.error(LogAction.HTTP_LOG + "error exist path :" + path);
//					return false;
//				}
//			}
//		}

		/**
		 * 컨텐츠 폴더 삭제
		 */
		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
		for(String dir : dirs) {
			
			String path = makeProviderPath(dir, provider);
			String disablePath = makeProviderDisablePath(dir, provider);
			
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("/bin/rm");
			cmd.add("-rf");
			cmd.add(path);
			cmd.add(disablePath);
			
			log.debug(LogAction.COMMAND + cmd.toString());
			
			LinuxExecute exec = new LinuxExecute(conf, cmd);
			try {
				exec.run();
			} catch (Exception e) {
				log.error(e.toString());
				message = "provider path error";
				return false;
			}
			
			// 명령 실행 후 file 객체로 존재확인
			if(isExist(path) == true) {
				message = "not exist path error:" + path;
				log.error(LogAction.HTTP_LOG + "error exist path :" + path);
				return false;
			}
		}

		/**
		 * 기타 이미지, 원본, 작업 폴더 삭제
		 * snapshot, thumbnail, working
		 */
		ArrayList<String> userDir = conf.getHttpserverConf().getUserDir();
		for(int i = 0; i < userDir.size(); i++) {
			String dir = userDir.get(i);
			String path = makeProviderPath(dir, provider);
			
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("/bin/rm");
			cmd.add("-rf");
			cmd.add(path);
			
			log.debug(LogAction.COMMAND + cmd.toString());
			
			LinuxExecute exec = new LinuxExecute(conf, cmd);
			try {
				exec.run();
			} catch (Exception e) {
				log.error(e.toString());
				message = "provider path error";
				return false;
			}
			
			// 명령 실행 후 file 객체로 존재확인
			if(isExist(path) == true) {
				log.error(LogAction.HTTP_LOG + "error exist path :" + path);
				return false;
			}		
		}

		return true;
	}


	private boolean cmdRegister(Map<String, Object> params, String result) {
		
		String provider = (String) params.get(PARAMS.PROVIDER);

		/*
		 * 경로 생성을 FTP가 자동으로 처리하도록 추가함
		// ftp path create
		ArrayList<String> array = conf.getHttpserverConf().getFtpDir();
		for(int i = 0; i < array.size(); i++) {
			String ftpRoot = array.get(i);
			String path = makeFtpPath(ftpRoot, provider);
			
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("/bin/mkdir");
			cmd.add(path);
			
			log.debug(LogAction.COMMAND + cmd.toString());
			
			LinuxExecute exec = new LinuxExecute(conf, cmd);
			try {
				exec.run();
			} catch (Exception e) {
				log.error(e.toString());
				message = "provider path error";
				return false;
			}
			
			if(!isWindows) {
				// 명령 실행 후 file 객체로 존재확인
				if(isExist(path) == false) {
					message = "path error: " + path;
					log.error(LogAction.HTTP_LOG + "error exist path :" + path);
					return false;
				}
			}
		}
		*/
		
		// 컨텐츠 경로 생성
		ArrayList<String> dirs = conf.getHttpserverConf().getContentDir();
		for(String dir : dirs) {
			
			String path = makeProviderPath(dir, provider);
			
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("/bin/mkdir");
			cmd.add(path);
			
			log.debug(LogAction.COMMAND + cmd.toString());
			
			LinuxExecute exec = new LinuxExecute(conf, cmd);
			try {
				exec.run();
			} catch (Exception e) {
				log.error(e.toString());
				message = "provider path error";
				return false;
			}
			
			if(!isWindows) {
				// 명령 실행 후 file 객체로 존재확인
				if(isExist(path) == false) {
					message = "path error: " + path;
					log.error(LogAction.HTTP_LOG + "error exist path :" + path);
					return false;
				}
			}
		}
		
		return cmdEnable(params, result);
	}

	@Override
	protected boolean checkParams(Map<String, Object> params) {
		if(checkParam(params, PARAMS.COMMAND) == false) return false;
		if(checkParam(params, PARAMS.PROVIDER) == false) return false;
		return true;
	}
	
	
	@SuppressWarnings("unused")
	private String makeResult(int error, String message) {
		return "";
	}

}
