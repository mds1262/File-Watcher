package net.catenoid.watcher.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * 
 * @author FIREWOLF
 *
 */
public class FileCopyCommander {
	
	private static Logger log = Logger.getLogger(FileCopyCommander.class);
	
    private static final int BUFFER_SIZE = 2048;
	private boolean isUseRsync = false;
	private String rsyncBandwidthLimit = null;

	/**
	 * rsync 경로 : /usr/bin/rsync 
	 * @param useRsync rsync를 사용하려는 경우 true
	 * @param rsyncBandwidthLimit
	 */
	public FileCopyCommander(boolean useRsync, String rsyncBandwidthLimit) {
		this.isUseRsync = useRsync;
		this.rsyncBandwidthLimit = rsyncBandwidthLimit;
	}
	
	class InputHandler extends Thread {

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
			log.debug(line);
		}
		
	}

	private Process childProcess = null;

	/**
	 * 파일 복사 스크립트를 실행한다.
	 * @return
	 * @throws Exception
	 */
	public boolean run(String src, String dest) throws Exception {
		
		ArrayList<String> array =  new ArrayList<String>();
		
		if(this.isUseRsync) {
			array.add("/usr/bin/rsync");
	    	if(this.rsyncBandwidthLimit != null) {
	    		array.add(this.rsyncBandwidthLimit);
	    	}
		} else {
			array.add("/bin/cp");
		}
    	array.add(src);
    	array.add(dest);
		
    	log.debug("start : " + array.toString());
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
		log.debug("end : " + array.toString());
				
		destroy();
		
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