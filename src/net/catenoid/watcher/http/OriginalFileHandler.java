package net.catenoid.watcher.http;

import java.util.ArrayList;
import java.util.Map;

import net.catenoid.watcher.LogAction;

import org.apache.log4j.Logger;

import com.kollus.utils.myUtils;

public class OriginalFileHandler extends HandlerExt {

	private static Logger log = Logger.getLogger(OriginalFileHandler.class);
		
	private interface PARAMS {
		public final String COMMAND = "cmd";
		public final String PROVIDER = "provider";
		public final String ORIGIN_PATH = "origin_path";
	}

	@Override
	protected boolean runCommand(Map<String, Object> params, String result) {
		String command = (String) params.get(PARAMS.COMMAND);
		switch(command) {
		case "remove" : return cmdRemove(params, result);
		}
		return false;
	}

	private boolean cmdRemove(Map<String, Object> params, String result) {

		long nowmil = System.currentTimeMillis();
		
		String provider = (String) params.get(PARAMS.PROVIDER);		
		String origin_path = (String) params.get(PARAMS.ORIGIN_PATH);
		
		/**
		 * origin_path가 6자 이하일수는 없을 것이기 때문에 사용함 
		 */
		if(origin_path != null && origin_path.length() > 6) {
			if(delete_origin_path(provider, origin_path) == false) {
				return false;
			}

			log.debug(LogAction.COMMAND + String.format("Original files delete elimination time: %d ms", System.currentTimeMillis() - nowmil));
			return true;
		}
		
		return false;
	}
	
	private boolean delete_origin_path(String provider, String backupFile) {
		
		int existCount = 0;
		ArrayList<String> backupDir = conf.getHttpserverConf().getBackupDir();
		for(String dir : backupDir) {

			String filename = makeBackupContentFile(dir, backupFile);
			filename = myUtils.FilenameDefence(filename);
			
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
		
	@Override
	protected boolean checkParams(Map<String, Object> params) {
		if(checkParam(params, PARAMS.COMMAND) == false) return false;
		if(checkParam(params, PARAMS.PROVIDER) == false) return false;
		if(checkParam(params, PARAMS.ORIGIN_PATH) == false) return false;
		
		return true;
	}
	
	private String makeResult(int error, String message) {
		return "";
	}

}
