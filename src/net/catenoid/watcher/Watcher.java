package net.catenoid.watcher;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.HttpServerConf;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.http.MediaContentHandler;
import net.catenoid.watcher.http.OriginalFileHandler;
import net.catenoid.watcher.http.ParameterFilter;
import net.catenoid.watcher.http.ProviderHandler;
import net.catenoid.watcher.http.TranscodingFileHandler;
import net.catenoid.watcher.http.WorkingFileHandler;
import net.catenoid.watcher.job.Role_Watcher;
import net.catenoid.watcher.utils.WatcherUtils;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

public class Watcher {
	
	private static Logger log = Logger.getLogger(Watcher.class);
	public static String VERSION = getManifestInfo();
//	public static String VERSION ="1.6-b415 [gson]";
 
	/**
	 * 프로그램 버전 출력
	 */
	public static void printVersion() {
		log.info(LogAction.PROGRAM_VERSION + Watcher.VERSION);
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String user_dir = System.getProperty("user.dir");

		String[] paths = {
				user_dir + "/log4j.properties",
				user_dir + "/bin/log4j.properties",
				user_dir + "/conf/log4j.properties"
		};
		
		for(String path : paths) {
			File propFile = new File(path);
			if (propFile.exists()) {
				PropertyConfigurator.configureAndWatch(path, 60000L);
			}
		}
		
//		PropertyConfigurator.configure( path + "/log4j.properties" );
		
		String mode = null;

		for(int i=0;args != null && i < args.length; i++) {
			if(args[i].startsWith("start")){
				mode = args[i];

			}else if(args[i].startsWith("stop")){
				mode = args[i];
			}
		}

		if ("start".equals(mode)){
			try {
				start();
			} catch(Exception e) {
				log.error(WatcherUtils.getStackTrace(e));
			}
		}else if("stop".equals(mode)){
			stop();
		}

	}
	
	public static void start() throws Exception {
		
		log.info(LogAction.PROGRAM_START);
		
		/**
		 * 프로그램 버전 출력
		 */
		printVersion();
		
		/**
		 * 초기화 및 환경 정보 확인 작업
		 */		
		Config conf = Config.getConfig();

		/**
		 * 시작과 함께 모든 Watcher를 한번 실행하도록 옵션이 설정된 경우
		 */
		if(conf.isStartWithRun() == true) {
			runOneTimeWatcher(conf.getWatchers());
		}
		
		/**
		 * 환경설정 정보에 따라 JobThread 생성
		 */
		int runJobs = 0;
		if(setupWatcher(conf) == true) {
			runJobs++;
		}
		 
		if(runJobs == 0) {
			log.error(LogAction.PROGRAM_CONFIG + Config.WATCHER_PROPERTIES_FILE);
			
			SchedulerFactory schedulerFactory = new StdSchedulerFactory();
			Scheduler scheduler = schedulerFactory.getScheduler();
			scheduler.pauseAll();
			List<JobExecutionContext> jobCtx = scheduler.getCurrentlyExecutingJobs();
			for(JobExecutionContext ctx : jobCtx) {
				JobKey jKey = ctx.getJobDetail().getKey();
				scheduler.interrupt(jKey);
			}
			scheduler.shutdown(true);
		}

		/*
		 * main 함수가 종료되어도 Job Thread가 종료될때 까지 프로그램은 계속 동작함.
		 */
		
		if(conf.getHttpserverConf() != null && conf.getHttpserverConf().isEnabled() == true) {
			setupHttpServer(conf);
		}
		
	}

	private static void setupHttpServer(Config conf) {
		HttpServerConf httpConf = conf.getHttpserverConf();
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(httpConf.get_port()), 1024);
		    
			HttpContext providerContext = server.createContext("/provider", new ProviderHandler());
			providerContext.getFilters().add(new ParameterFilter());

			HttpContext contentContext = server.createContext("/transcoding_file", new TranscodingFileHandler());
			contentContext.getFilters().add(new ParameterFilter());

			HttpContext mediaContext = server.createContext("/media_content", new MediaContentHandler());
			mediaContext.getFilters().add(new ParameterFilter());
			
			HttpContext workingFileContext = server.createContext("/working_file", new WorkingFileHandler()); 
			workingFileContext.getFilters().add(new ParameterFilter()); 

			HttpContext originalFileContext = server.createContext("/original_file", new OriginalFileHandler());
			originalFileContext.getFilters().add(new ParameterFilter());

			server.start();
		    log.info(LogAction.HTTP_SERVER_START);
		} catch (IOException e) {
			log.error(e.toString());
		}
	}	

	public static void stop() {
		
		SchedulerFactory schedulerFactory = new StdSchedulerFactory();
		Scheduler scheduler;
		try {
			scheduler = schedulerFactory.getScheduler();
			scheduler.pauseAll();
			scheduler.shutdown(true);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 시작과 동시에 watcher를 한번씩 실행하도록 옵션을 설정한 경우 실행된다.
	 * @param watchers
	 * @throws SchedulerException
	 */
	private static void runOneTimeWatcher(WatcherFolder[] watchers) throws SchedulerException {

		if(watchers == null || watchers.length == 0)
			return;

		for(WatcherFolder info : watchers) {
			if(info.isEnabled()) {
				Role_Watcher watcher = new Role_Watcher();
				watcher.run(info);
			}
		}
	}

	/**
	 * 감시 작업 리스트를 각 작업으로 등록한다.
	 * @param conf
	 * @return
	 * @throws SchedulerException
	 */
	private static Boolean setupWatcher(Config conf) throws SchedulerException {
		
		WatcherFolder[] watchers = conf.getWatchers();
		if(watchers == null || watchers.length == 0)
			return false;

		for(WatcherFolder info : watchers) {
			if(info.isEnabled()) {
				SchedulerFactory schedulerFactory = new StdSchedulerFactory();
				Scheduler scheduler = schedulerFactory.getScheduler();
				
				scheduler.start();
	
				String strTriggerName = info.getTriggerName();
				String strGroupName = info.getGroupName();
				String strJobName = info.getJobName();
				
				JobDetail job = newJob(Role_Watcher.class)
						.withIdentity(strJobName)
					    .build();
				
				job.getJobDataMap().put("data", info);
				
				log.debug(LogAction.PROGRAM_CONFIG + strJobName + "=" + info.getInterval());
				
				CronTrigger trigger = newTrigger()
						.withIdentity(strTriggerName, strGroupName)
						.withSchedule(cronSchedule(info.getInterval()))
						.forJob(strJobName)
						.build();
				
				scheduler.scheduleJob(job, trigger);
			}
		}

		return true;		
	}
	
	public static String getManifestInfo() {
	    Enumeration resEnum;
	    try {
	        resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
	        while (resEnum.hasMoreElements()) {
	            try {
	                URL url = (URL)resEnum.nextElement();
	                InputStream is = url.openStream();
	                if (is != null) {
	                    Manifest manifest = new Manifest(is);
	                    Attributes mainAttribs = manifest.getMainAttributes();
	                    String version = mainAttribs.getValue("MediaWatcher2-Version");
	                    if(version != null) {
	                        return version;
	                    }
	                }
	            }
	            catch (Exception e) {
	                // Silently ignore wrong manifests on classpath?
	            }
	        }
	    } catch (IOException e1) {
	        // Silently ignore wrong manifests on classpath?
	    }
	    return null; 
	}
}
