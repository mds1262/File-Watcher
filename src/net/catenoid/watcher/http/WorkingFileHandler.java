package net.catenoid.watcher.http;

import java.util.ArrayList;
import java.util.Map;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.utils.WatcherUtils;

import org.apache.log4j.Logger;

public class WorkingFileHandler extends HandlerExt {

	private static Logger log = Logger.getLogger(WorkingFileHandler.class);
		
	private interface PARAMS {
		public final String COMMAND = "cmd";
		public final String PROVIDER = "provider";
		public final String CONTENT_PATH = "content_path";
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

		@SuppressWarnings("unused")
		String provider = (String) params.get(PARAMS.PROVIDER);		
		String content_path = (String) params.get(PARAMS.CONTENT_PATH);
		content_path = WatcherUtils.FilenameDefence(content_path);
		
		// delete media_content
		String dir = conf.getHttpserverConf().getWorkingDir();

		// rm -rf /mnt/medianas/transcoding_file/catenodi/yyyymmdd/media_content_id/
		String path = makeWorkingFilePath(dir, content_path);
	
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("/bin/rm");
//		cmd.add("-rf");
		cmd.add(path);
		
		log.debug(LogAction.COMMAND + cmd);
		
		LinuxExecute exec = new LinuxExecute(conf, cmd);
		exec.enableStdOutOff(true); // 파일 직접 확인하니까 output off
		try {
			exec.run();
		} catch (Exception e) {
			log.error(e.toString());
			return false;
		}
		
		if(!isWindows) {
			// 명령 실행 후 file 객체로 존재확인
			if(isExist(path) == true) {
				log.error(LogAction.HTTP_LOG + "error exist path :" + path);
				return false;
			}
		}

		log.debug(LogAction.COMMAND + String.format("Working file delete elimination time: %d ms", System.currentTimeMillis() - nowmil));
		return true;
	}
		
	@Override
	protected boolean checkParams(Map<String, Object> params) {
		if(checkParam(params, PARAMS.COMMAND) == false) return false;
		if(checkParam(params, PARAMS.PROVIDER) == false) return false;
		if(checkParam(params, PARAMS.CONTENT_PATH) == false) return false;
		
		return true;
	}
	
	@SuppressWarnings("unused")
	private String makeResult(int error, String message) {
		return "";
	}

}
