package net.catenoid.watcher.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.Watcher;
import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.upload.config.LineParser;
import net.catenoid.watcher.upload.config.LinuxLineParser;
import net.catenoid.watcher.upload.config.WindowsLineParser;
import net.catenoid.watcher.upload.dto.FileItemDTO;

/**
 * 
 * @author FIREWOLF
 *
 */
public class LinuxExecute {
	
	private static Logger log = Logger.getLogger(LinuxExecute.class);
    private ArrayList<String> _cmdArray = null;		
    private boolean stdout_off = false;
    
    static boolean isWindows = false;
    static
    {
    	try {
    		String os=System.getProperty("os.name");
    		if (os!=null && os.toLowerCase().startsWith("windows")) {
    			isWindows = true;
    		}
    	} catch (LinkageError  e) {
    		isWindows = true;
    	}
    }

    public void enableStdOutOff(boolean stdoutOff) {
    	this.stdout_off = stdoutOff;
    }
    
	public LinuxExecute(Config config, ArrayList<String> cmds) {
		_cmdArray = cmds;
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
			if(!stdout_off) log.trace(LogAction.SH_STDOUT + line);
		}
		
	}

	private Process childProcess = null;

	/**
	 * 썸네일 획득을 위해 FFMPEG 프로세스를 실행한다.
	 * @return
	 * @throws Exception
	 */
	public boolean run() throws Exception {
		
		if(isWindows) {
			log.error("linux only service");
		} else {
			try
			{
				String[] cmdArray = new String[_cmdArray.size()];
				_cmdArray.toArray(cmdArray);
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
	

	private void destroy()
	{
		if(childProcess != null)
		{
			childProcess.destroy();
		}
	}

}
