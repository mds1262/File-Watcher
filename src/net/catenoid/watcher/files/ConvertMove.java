package net.catenoid.watcher.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.config.Config;

/**
 * 
 * @author FIREWOLF
 * 
 */
public class ConvertMove {

	private static Logger log = Logger.getLogger(ConvertMove.class);

	static boolean isWindows = false;

	private static final int BUFFER_SIZE = 2048;
	private String rootPath;
	private Config conf;

	public ConvertMove(Config config, String strRoot) {
		this.rootPath = strRoot;
		this.conf = config;
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
			try {
				reader = new BufferedReader(new InputStreamReader(input_), BUFFER_SIZE);
				String line;

				while ((line = reader.readLine()) != null) {
					onNewline(line);
					mLines++;

					/**
					 * 혹시 MAX_VALUE 보다 크다면 0으로 초기화
					 */
					if (mLines == Integer.MAX_VALUE)
						mLines = 0;
				}

			} catch (IOException e) {
			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e) {
					}

				stopCatter();
			}
		}

		private void stopCatter() {
			if (childProcess != null) {
				childProcess.destroy();
			}
		}

		private void onNewline(String line) {
			log.debug(LogAction.SH_STDOUT + line);
		}

	}

	private Process childProcess = null;

	/**
	 * 썸네일 획득을 위해 FFMPEG 프로세스를 실행한다.
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean run() throws Exception {

		String convPath = conf.get_convmv_path();

		if(convPath != null && !convPath.isEmpty()) {
			ArrayList<String> array =  new ArrayList<String>();
			array.add(convPath);
	    	array.add("-f");
	    	array.add(conf.getDefaultCharset());
	    	array.add("-t");
	    	array.add("utf8");
	    	array.add("-r");
	    	array.add("--notest");
	    	array.add(rootPath);
			
			try
			{
				String[] cmdArray = new String[array.size()];
				array.toArray(cmdArray);
				
				ProcessBuilder pb = new ProcessBuilder(cmdArray);
				
				childProcess = pb.start();
				InputHandler errorHandler = new InputHandler(childProcess.getErrorStream(), "Error Stream");
		        errorHandler.start();
		        InputHandler inputHandler = new InputHandler(childProcess.getInputStream(), "Output Stream");
		        inputHandler.start();
		        childProcess.waitFor();
		        
				destroy();
				
			} catch (Exception e) {
				destroy();
				throw e;
			}					
			destroy();
		}
		
		return true;
	}

	private void destroy() {
		if (childProcess != null) {
			childProcess.destroy();
		}
	}
}
