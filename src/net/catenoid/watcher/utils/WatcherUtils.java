package net.catenoid.watcher.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class WatcherUtils {

	private static Logger log = Logger.getLogger(WatcherUtils.class);

//	public static String millisecoundsToTimeformat(long millis) {
//		return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
//				TimeUnit.MILLISECONDS.toMinutes(millis)
//						- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
//				TimeUnit.MILLISECONDS.toSeconds(millis)
//						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
//	}

	public final static long ONE_SECOND = 1000;
	public final static long SECONDS = 60;
	public final static long ONE_MINUTE = ONE_SECOND * 60;
	public final static long MINUTES = 60;
	public final static long ONE_HOUR = ONE_MINUTE * 60;
	public final static long HOURS = 24;
	public final static long ONE_DAY = ONE_HOUR * 24;
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * 밀리초를 00:00:00 포멧 문자열로 변환
	 * 
	 * @param duration
	 * @return
	 */
	public static String millisToDHMS(long duration) {
		String res = "";
		duration /= ONE_SECOND;
		int seconds = (int) (duration % SECONDS);
		duration /= SECONDS;
		int minutes = (int) (duration % MINUTES);
		duration /= MINUTES;
		int hours = (int) (duration % HOURS);
		int days = (int) (duration / HOURS);
		if (days == 0) {
			if (hours == 0) {
				res = String.format("%02d:%02d", minutes, seconds);
			} else {
				res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
			}
		} else {
			res = String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
		}
		return res;
	}

	/**
	 * 초를 00:00:00 포멧 문자열로 변환
	 * 
	 * @param duration
	 * @return
	 */
//	public static String secondsToDHMS(long duration) {
//		String res = "";
//		int seconds = (int) (duration % SECONDS);
//		duration /= SECONDS;
//		int minutes = (int) (duration % MINUTES);
//		duration /= MINUTES;
//		int hours = (int) (duration % HOURS);
//		int days = (int) (duration / HOURS);
//		if (days == 0) {
//			if (hours == 0) {
//				res = String.format("%02d:%02d", minutes, seconds);
//			} else {
//				res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
//			}
//		} else {
//			res = String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
//		}
//		return res;
//	}

	/**
	 * 밀리초를 00:00:00 포멧 문자열로 변환
	 * 
	 * @param duration
	 * @return
	 */
//	public static String millisToString(long duration) {
//		String res = "";
//		duration /= ONE_SECOND;
//		int seconds = (int) (duration % SECONDS);
//		duration /= SECONDS;
//		int minutes = (int) (duration % MINUTES);
//		duration /= MINUTES;
//		int hours = (int) duration;
//		res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
//		return res;
//	}

	/**
	 * 초를 00:00:00 포멧 문자열로 변환
	 * 
	 * @param duration
	 * @return
	 */
	public static String secondsToString(int duration) {
		String res = "";
		int seconds = (int) (duration % SECONDS);
		duration /= SECONDS;
		int minutes = (int) (duration % MINUTES);
		duration /= MINUTES;
		int hours = (int) duration;
		res = String.format("%02d:%02d:%02d", hours, minutes, seconds);
		return res;
	}

	/**
	 * 문자열이 빈공간인지 확인한다. zero 문자열 포함
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str) {
		if (str == null)
			return true;
		if (str.trim().length() == 0)
			return true;
		return false;
	}

	/**
	 * 파일 확장자를 획득한다.
	 * 
	 * @param filepath
	 * @return
	 */
	public static String getFileExtention(String filepath) {
		File f = new File(filepath);
		int idx = f.getName().lastIndexOf('.');
		String ext = (idx > 0) ? f.getName().substring(idx) : "";
		return ext;
	}

	/**
	 * long형 날짜를 문자열로 변환한다.
	 * 
	 * @param time
	 * @return
	 */
	public static String getDateString(long time) {
		Date d = new Date();
		d.setTime(time);
		return dateFormat.format(d);
	}

	/**
	 * 파일을 이동시킨다. 이동시킬수 없는 경우도 존재함
	 * 
	 * @param fromFile
	 * @param toFile
	 * @return
	 * @throws IOException
	 */
//	public static boolean rename(File fromFile, File toFile) throws IOException {
//		if (fromFile.isDirectory()) {
//			File[] files = fromFile.listFiles();
//			if (files == null) {
//				// 디렉토리 내 아무것도 없는 경우
//				return fromFile.renameTo(toFile);
//			} else {
//				// 디렉토리내 파일 또는 디렉토리가 존재하는 경우
//				if (!toFile.mkdirs()) {
//					return false;
//				}
//				for (File eachFile : files) {
//					File toFileChild = new File(toFile, eachFile.getName());
//					if (eachFile.isDirectory()) {
//						if (!rename(eachFile, toFileChild)) {
//							return false;
//						}
//					} else {
//						if (!eachFile.renameTo(toFileChild)) {
//							return false;
//						}
//					}
//				}
//				return fromFile.delete();
//			}
//		} else {
//			// 파일인 경우
//			if (fromFile.getParent() != null) {
//				String fromPath = fromFile.getParent();
//				String toPath = toFile.getParent();
//
//				if (fromPath.compareTo(toPath) != 0) {
//					File tempPath = new File(toPath);
//					tempPath.mkdirs();
//				}
//			}
//			return fromFile.renameTo(toFile);
//		}
//	}

	/**
	 * 해당 파일의 경로의 부모 폴더를 생성합니다.
	 * 
	 * @param destPath
	 * @return
	 */
//	public static void makePath(String destPath) {
//		File destFile = new File(destPath);
//		String toPath = destFile.getParent();
//
//		File tempPath = new File(toPath);
//		tempPath.mkdirs();
//		/**
//		 * 폴더 생성에 성공하면 설정을 777로 변경합니다.
//		 */
//		chmod777(toPath);
//	}

	/**
	 * NIO 파일 Copy
	 * 
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
//	public static void copyFile(File sourceFile, File destFile) throws IOException {
//		if (!destFile.exists()) {
//			destFile.createNewFile();
//		}
//
//		FileChannel source = null;
//		FileChannel destination = null;
//		try {
//			source = new FileInputStream(sourceFile).getChannel();
//			destination = new FileOutputStream(destFile).getChannel();
//			destination.transferFrom(source, 0, source.size());
//		} finally {
//			if (source != null) {
//				source.close();
//			}
//			if (destination != null) {
//				destination.close();
//			}
//		}
//	}

	/**
	 * HashMap을 디버깅을 위해 출력하는 함수 <br>
	 * log.debug로 출력한다.
	 * 
	 * @param map
	 */
//	public static void printMap(HashMap<String, String> map) {
//		Iterator<String> keys = map.keySet().iterator();
//		while (keys.hasNext()) {
//			String name = keys.next();
//			String value = map.get(name);
//		}
//	}

//	public static String getArrayString(String[] str) {
//		StringBuilder sb = new StringBuilder();
//		for (int i = 0; i < str.length; i++) {
//			sb.append(str[i]);
//		}
//		return sb.toString();
//	}

	/**
	 * filename에서 고객사의 도메인명을 획득한다.
	 * 
	 * @param watcher_dir
	 * @param filename
	 * @return
	 */
	public static String getDomain(String watcher_dir, String filename) {

		String name = filename.toLowerCase();
		name = name.replace("\\", "/");

		watcher_dir = watcher_dir.toLowerCase();
		watcher_dir = watcher_dir.replace("\\", "/");

		String path = name.substring(watcher_dir.length());
		int pos = path.indexOf("/", 1);

		if (pos == -1)
			return null;
		String domain = path.substring(1, pos);

		return domain;
	}

	/**
	 * filename 에서 watcher_dir을 제외한 업로드 PATH를 생성한다.
	 * 
	 * @param watcher_dir
	 * @param filename
	 * @return
	 */
	public static String getUploadPath(String watcher_dir, String filename) {

		String name = filename;// .toLowerCase();
		name = name.replace("\\", "/");

		watcher_dir = watcher_dir.toLowerCase();
		watcher_dir = watcher_dir.replace("\\", "/");

		/**
		 * length까지만 잘라나면 path의 시작이 / 이므로 +1 해서 하나더 잘라낸다.
		 */
		String path = name.substring(watcher_dir.length() + 1);

		/**
		 * path가 / 로 시작하지 않는 경우 /를 붙여준다.
		 */
		if (path.charAt(0) != '/') {
			path = "/" + path;
		}

		return path.replace("\\", "/");
	}

//	public static String getCategoryPath(String upload_path) {
//		return "";
//	}

	public static String getWithoutDomain(String watcher_dir, String filename) {
		int pos = filename.indexOf("/", 1);
		return filename.substring(pos + 1);
	}

	/**
	 * path의 모든 하위 폴더를 삭제한다.
	 * 
	 * @param path
	 */
//	public static void deleteFolder(String path) {
//		File root = new File(path);
//		File[] files = root.listFiles();
//		for (File file : files) {
//			if (file.isDirectory()) {
//				if (_deleteFolder(file) == false) {
//					log.error("delete Folder Error: " + file.getAbsolutePath());
//				}
//			}
//		}
//	}

	/**
	 * targetFolder의 내부 파일과 해당 폴더를 삭제한다.<br>
	 * 
	 * @param targetFolder
	 * @return
	 */
//	private static boolean _deleteFolder(File targetFolder) {
//		File[] childFile = targetFolder.listFiles();
//		int size = childFile.length;
//		if (size > 0) {
//			for (int i = 0; i < size; i++) {
//				if (childFile[i].isFile()) {
//					childFile[i].delete();
//				} else {
//					if (_deleteFolder(childFile[i]) == false) {
//						log.error("delete Folder Error: " + childFile[i].getAbsolutePath());
//					}
//				}
//			}
//		}
//		targetFolder.delete();
//		return (!targetFolder.exists());
//	}

//	public static String human_readable_size(float size, int offset) {
//		float mod = 1000;
//		String units[] = { "B", "KB", "MB", "GB" };
//		int i = offset;
//		for (; size > mod && i < units.length; i++) {
//			size /= mod;
//		}
//		size = Float.parseFloat(String.format("%.2f", size));
//		return "" + size + units[i];
//	}

//	public static Long ipToInt(String addr) {
//		String[] addrArray = addr.split("\\.");
//		long num = 0;
//		for (int i = 0; i < addrArray.length; i++) {
//			int power = 3 - i;
//			num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
//		}
//		return num;
//	}

//	public static long ipToLong(String ip) {
//		long result = 0;
//		try {
//			result = ipToLong(InetAddress.getByName(ip));
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}
//		return result;
//	}

//	public static long ipToLong(InetAddress ip) {
//		byte[] octets = ip.getAddress();
//		long result = 0;
//		for (byte octet : octets) {
//			result <<= 8;
//			result |= octet & 0xff;
//		}
//		return result;
//	}

//	public static String implodeArray(String[] inputArray, String glueString) {
//
//		/** Output variable */
//		String output = "";
//
//		if (inputArray.length > 0) {
//			StringBuilder sb = new StringBuilder();
//			sb.append(inputArray[0]);
//
//			for (int i = 1; i < inputArray.length; i++) {
//				sb.append(glueString);
//				sb.append(inputArray[i]);
//			}
//
//			output = sb.toString();
//		}
//
//		return output;
//	}

	public static String implodeList(List<String> inputArray, String glueString) {

		/** Output variable */
		String output = "";

		if (inputArray.size() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(inputArray.get(0));

			for (int i = 1; i < inputArray.size(); i++) {
				sb.append(glueString);
				sb.append(inputArray.get(i));
			}

			output = sb.toString();
		}

		return output;
	}

//	public static ArrayList<String> splitPath(String upload_path) {
//		String[] paths = upload_path.split("/");
//		return new ArrayList<String>(Arrays.asList(paths));
//	}

	/**
	 * snapshot image chmod 777 변경
	 */
	public static void chmod777(String filepath) {

		// using PosixFilePermission to set file permissions 777
		Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
		// add owners permission
		perms.add(PosixFilePermission.OWNER_READ);
		perms.add(PosixFilePermission.OWNER_WRITE);
		perms.add(PosixFilePermission.OWNER_EXECUTE);

		// add group permissions
		perms.add(PosixFilePermission.GROUP_READ);
		perms.add(PosixFilePermission.GROUP_WRITE);
		perms.add(PosixFilePermission.GROUP_EXECUTE);

		// add others permissions
		perms.add(PosixFilePermission.OTHERS_READ);
		perms.add(PosixFilePermission.OTHERS_WRITE);
		perms.add(PosixFilePermission.OTHERS_EXECUTE);

		try {
			Files.setPosixFilePermissions(Paths.get(filepath), perms);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 사용자가 root를 임의로 변경해서 발생할 수 있는 오류를 방지하기 위해 파일명에서 사용할 수 없는 내용을 삭제하여 반환하는 함수
	 * 
	 * @param filename
	 * @return
	 */
	public static String FilenameDefence(String filename) {
		String out = filename;
		out = out.replaceAll("\\/\\.\\.", "");
		out = out.replaceAll("\\/\\.", "");
		out = out.replaceAll("^~", "");
		return out;
	}

	public static String getStackTrace(Exception e) {
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		return errors.toString();
	}
}
