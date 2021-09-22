package net.catenoid.watcher.http;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;

import net.catenoid.watcher.LogAction;
import net.catenoid.watcher.Watcher;
import net.catenoid.watcher.config.Config;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerExt implements HttpHandler {
	
	private static Logger log = Logger.getLogger(HandlerExt.class);
	
	protected Config conf = null;
	protected String message;
	
	protected static boolean isWindows = false;
	protected boolean isDebug = false;
	protected static String DEFAULT_CHARSET = "UTF-8";
	
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
    
    /**
     * [source_path]/[provider] %s/%s
     * @param ftpRoot
     * @param provider
     * @return
     */
    protected String makeFtpPath(String ftpRoot, String provider) {
		return String.format("%s/%s", ftpRoot, provider);
	}
	
    /**
     * [source_path]/[provider]/[provider]_ %s/%s/%s_
     * @param w3Path
     * @param provider
     * @return
     */
	protected String makeProviderDisablePath(String w3Path, String provider) {
		return String.format("%s/%s_", w3Path, provider);
	}
	
	/**
	 * 파일 존재 확인
	 * @param path 
	 * @return
	 */
    protected boolean isExist(String path) {
		File f = new File(path);
		return f.exists();
	}


	/**
	 * [source_path]/[provider]
	 * %s/%s
	 * 
	 * @param sourcePath
	 * @param provider
	 * @return
	 */
	protected String makeProviderPath(String sourcePath, String provider) {
		return String.format("%s/%s", sourcePath, provider);
	}
	
	/**
	 * content_path에 / 이 없으면 붙여줌 <br>
	 * [source_path]/[content_path : with provider] %s%s <br>
	 * /mnt/medianas/transcoding_file/catenoid/20121109/20
	 * 
	 * @param sourcePath
	 * @param content_path
	 * @return
	 */
	protected String makeContentPath(String sourcePath, String content_path) {
		if(content_path.charAt(0) != '/') {
			content_path = "/" + content_path;
		}
		return String.format("%s%s", sourcePath, content_path);
	}

	protected String makeWorkingFilePath(String sourcePath, String content_path) {
		if(content_path.charAt(0) != '/') {
			content_path = "/" + content_path;
		}
		return String.format("%s%s", sourcePath, content_path);
	}

	/**
	 * content_path에 / 이 없으면 붙여줌 <br>
	 * [source_path]/[content_path : with provider]_
	 * 
	 * @param sourcePath
	 * @param content_path
	 * @return
	 */
	protected String makeDisableContentPath(String sourcePath, String content_path) {
		if(content_path.charAt(0) != '/') {
			content_path = "/" + content_path;
		}
		return String.format("%s%s_", sourcePath, content_path);
	}
	
	/**
	 * 원본 패키지 경로 <br>
	 * [www_source_path]/[provider]/[package_key]   %s/%s/%s <br>
	 * /mnt/medianas/transcoding_file/[provider]/[package_key]
	 * 
	 * @param sourcePath
	 * @param provider
	 * @param package_key
	 * @return
	 */
	protected String makeSourcePackagePath(String sourcePath, String provider, String package_key) {
		return String.format("%s/%s/%s", sourcePath, provider, package_key);
	}

	/**
	 * 원본 패키지 경로 <br>
	 * [www_source_path]/[provider]/[package_key]   %s/%s/%s <br>
	 * /mnt/medianas/transcoding_file/[provider]/[package_key]_
	 * 
	 * @param sourcePath
	 * @param provider
	 * @param package_key
	 * @return
	 */
	protected String makeDisablePackagePath(String sourcePath, String provider, String package_key) {
		return String.format("%s/%s/%s_", sourcePath, provider, package_key);
	}
	
	/**
	 * 위임된 패키지 경로 <br>
	 * 
	 * [www_source_path]/[provider]/[package_key]@[dist]   %s/%s/%s@%s <br>
	 * /mnt/nedianas/transcoding_file/[owner]/[package_key]@[dist]
	 * 
	 * @param sourcePath
	 * @param owner
	 * @param package_key
	 * @param dist
	 * @return
	 */
	protected String makeDelegatePackagePath(String sourcePath, String owner, String package_key, String dist) {
		return String.format("%s/%s/%s@%s", sourcePath, owner, package_key, dist);
	}

	/**
	 * owner 자신의 package 경로<br>
	 * [www_source_path]/[owner]/[package_key]  %s/%s/%s
	 * 
	 * @param sourcePath
	 * @param owner
	 * @param package_key
	 * @return
	 */
	protected String makeMyPackagePath(String sourcePath, String owner, String package_key) {
		return String.format("%s/%s/%s", sourcePath, owner, package_key);
	}

	/**
	 * 패키지에 포함된 미디어 컨텐츠 링크 <br>
	 * [www_source_path]/[provider(owner)]/[package_key]/[media_content_id] %s/%s/%s/%s <br>
	 * /mnt/medianas/transcoding_file/catenoid/package_key/media_id
	 * 
	 * @param sourcePath
	 * @param owner
	 * @param package_key
	 * @param media_content_id
	 * @return
	 */
	protected String makePackageContentPath(String sourcePath, String owner, String package_key, String media_content_id) {
		return String.format("%s/%s/%s/%s", sourcePath, owner, package_key, media_content_id);
	}
	
	/**
	 * 백업파일 경로를 반환한다. <br>
	 *  /mnt/medianas/original/catenoid/20121122/202/~  => /mnt/medianas/original/catenoid/20121122/202
	 * @param dir
	 * @param backupFile
	 * @return
	 */
	protected String makeBackupContentPath(String dir, String backupFile) {
		String path = null;
		
		/*
		 * backup_path에서 뒤에서 부터 /를 찾는다.
		 * - 마지막이 /인 경우 /만 제거하고 돌려준다.
		 */
		if(backupFile.charAt(backupFile.length()-1) == '/') {
			path = backupFile.substring(0, backupFile.length()-1);
		} else {
			int pos = backupFile.lastIndexOf('/');
			if(pos != -1) {
				path = backupFile.substring(0, pos);
			} else {
				path = backupFile;
			}
		}
		
		return String.format("%s%s", dir, path);
	}

	/**
	 * 백업 컨텐츠 파일 경로 생성
	 * @param dir
	 * @param backupFile
	 * @return
	 */
	protected String makeBackupContentFile(String dir, String backupFile) {
		return String.format("%s%s", dir, backupFile);
	}
	
	protected boolean checkParam(Map<String, Object> params, String key) {
		if(params.containsKey(key) == false) {
			message = "params empty :" + key;
			return false;
		}
		String value = (String) params.get(key);
		if(value == null || value.length() == 0 || value.trim().length() == 0) {
			message = "params empty :" + key;
			return false;
		}
		return true;
	}

	protected void printMap(Map<String, Object> params) {
		if(params == null) {
			log.debug(LogAction.HTTP_LOG + "params empty");
			return;
		}

		Iterator iterator = params.keySet().iterator();		 
		while (iterator.hasNext()) {
		   String key = iterator.next().toString();
		   String value = params.get(key).toString();		 
		   log.debug(LogAction.HTTP_LOG + key + " " + value);
		}
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		long nowmil = System.currentTimeMillis();

		try {
			conf = Config.getConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		int retCode = 404;
		String result = "";
		InetSocketAddress _remoteAddr = exchange.getRemoteAddress();
		String remoteAddr = _remoteAddr.getAddress().toString();
		remoteAddr = remoteAddr.replace("/", "");
		
		String request_path = exchange.getRequestURI().getPath();
		log.debug(String.format("[request] %s / (remoteAddr) %s", request_path, remoteAddr));
		
		if(conf.getHttpserverConf().isAllowip(remoteAddr) == false) {
			retCode = 403;
			result = "permission denied";
			log.error("permission denied (403): " + remoteAddr);
		} else {
			Map<String, Object> params =
			           (Map<String, Object>)exchange.getAttribute("parameters");

			if(checkParams(params) == true) {
				if(runCommand(params, result) == true) {
					retCode = 200;
					if(message != null && message.length() > 0) result = message;
					else result = "OK";
				} else {
					retCode = 404; 
					result = message;
					String request_url = exchange.getRequestURI().toString();
					log.error(String.format("[%d] %s - %s", retCode, request_url, result));
				}
			} else {
				retCode = 403;
				result = message;				
				log.error(retCode + result);
			}
		}
		
		result += "\n";
		exchange.getResponseHeaders().set("KOLLUS_MEDIAWATCHER", Watcher.VERSION);
		exchange.sendResponseHeaders(retCode, result.length());
		
		OutputStream os = exchange.getResponseBody();
		os.write(result.getBytes());
		os.flush();
		os.close();
		
		exchange.close();
		
		log.debug(LogAction.COMMAND + String.format("%s HTTP Request elimination time: %d ms, result: %s", request_path, System.currentTimeMillis() - nowmil, message));

		message = "";
	}

	protected boolean runCommand(Map<String, Object> params, String result) {
		return false;
	}

	protected boolean checkParams(Map<String, Object> params) {
		return false;
	}

	/**
	 * 경로에서 확장자를 제거합니다.
	 * @param filename
	 * @return
	 */
	protected String removeExt(String filename) {
		return FilenameUtils.removeExtension(filename);
	}
}
