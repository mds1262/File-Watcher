import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


public class HelloWord {

	public static void main(String[] args) {
			
			ArrayList<String> array = new ArrayList<String>();
			array.add("ffmpeg");
			array.add("-report");
			array.add("-y");
			array.add("-ss");
			array.add("00:00:10");
			array.add("-i");
			array.add("/Users/sonjiyeong/TWICE-MerryHappy.mp4");
		
			array.add("-vframes");
			array.add("1");
			array.add("/Users/sonjiyeong/catenoid/MediaWatcher2/a.jpg");


			File file = new File("/Users/sonjiyeong/catenoid/MediaWatcher2/a.jpg");
			if(file.exists()) {
				file.delete();
			}
			
			try
			{	
				//childProcess = Runtime.getRuntime().exec(cmd);
				String[] cmdArray = new String[array.size()];
				array.toArray(cmdArray);
				System.out.println("snapshot cmd:"+Arrays.toString(cmdArray));
				ProcessBuilder pb = new ProcessBuilder(cmdArray);
				//pb.redirectErrorStream(true);
				
				Map<String, String> env = pb.environment();
				env.put("FFREPORT", String.format("file=/Users/sonjiyeong/catenoid/MediaWatcher2/history/%%p-%s.log:level=32", new SimpleDateFormat("YYYYMdd-HHmmssSSS").format(System.currentTimeMillis())));
				
				Process childProcess = pb.start();
//		        InputHandler inputHandler = new InputHandler(childProcess.getInputStream(), "Output Stream");
//		        inputHandler.start();
//		        InputHandler errorHandler = new InputHandler(childProcess.getErrorStream(), "Error Stream");
//		        errorHandler.start();
		        childProcess.waitFor();

				@SuppressWarnings("unused")
				
				int exitCode = childProcess.exitValue();
				System.out.println("exitCode: "+ exitCode);
				if (exitCode != 0) {
					System.out.println("exit code is not 0 [ "+ exitCode + "]");
				}
				@SuppressWarnings("unused")
				long _end = System.currentTimeMillis();
				
			} catch (Exception e) {
				e.printStackTrace();
				
			}
	
		}	
	

}
