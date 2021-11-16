package net.catenoid.watcher.upload.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.upload.dto.SendFileItemsDTO;

public class KusUploadUtils extends CommonUtils {
	private static Logger log = Logger.getLogger(KusUploadUtils.class);

	public KusUploadUtils(WatcherFolder info, Config conf) {
		// TODO Auto-generated constructor stub
		super(info, conf);
	}

	/*
	 * 실제 파일들의 전체경로 확인
	 */
	public void getUploadFullFilePath(File[] files, SendFileItemsDTO fileList, List<String> removeFiles) throws Exception {
		for (File f : files) {
			if (f.isFile()) {
				if (isCompleteFile(f.getName())) {

					FileItemDTO item = findWorkFileItems(f, false);

					fileList.add(item);

					log.debug("getUploadFullFilePath FileList : " + item.toString());

					if (removeFiles.size() > 0) {
						removeFiles.add(f.getPath());
						log.debug("getUploadFullFilePath RemoveFileList: " + f.getPath().toString());
					}
				} else if (fileNamePatternCheck(f.getName())) {
					if (isCompleteFileCopy(f.getPath())) {
						FileItemDTO item = findWorkFileItems(f, true);
						item.setConsoleUpload(true);
						fileList.add(item);

						log.debug("getUploadFullFilePath FileList : " + item.toString());
					}
				}
			}
		}
	}

	private FileItemDTO findWorkFileItems(File f, boolean isConsoleUpload) throws Exception {
		String pattern = "yyyy-MM-dd_hh:mm aa";
		FileItemDTO item = new FileItemDTO();
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);

		String originalFilePath = "";

		String formatData = "";
//		String originalFileName = f.getName().replaceAll("_complete","");

		long lastModifyDate = f.lastModified();
		Date d = new Date(lastModifyDate);
		formatData = sdf.format(d);
		Date fd = sdf.parse(formatData);
		formatData = "" + fd.getTime();
		long lastModified = Long.parseLong(formatData);
		
		originalFilePath = f.getPath().substring(0, f.getPath().length() - 9);
		if (isConsoleUpload) {
			originalFilePath = f.getPath();
		}
//		originalFilePath = f.getPath().replaceAll("_complete","");

		File oFile = new File(originalFilePath);

		long length = oFile.length();

		// 업로드파일키를 찾기
		String UploadFileKey = getUploadFileKey(f.getPath(), isConsoleUpload);

		// 기존 FileItem 멤버변수들의 값들의 get/set을 일치하기위해 fromResultSet을 사용하여서 처리
		// Upload RootFolder Path, fileOriginalFullPath, 최종수정일, Size
		item = CommonUtils.fromResultSet(null, info.getWatcherDir(), originalFilePath, lastModified, length, UploadFileKey);
		
		if (getMediaContentInfo(item)) {
			log.info("Success to extracted MediaInfo");
		}
		
		log.info("Find to item : " + item.toString());
		
		return item;
	}

	// 간략하게 폴더만 찾고싶을때 사용
	public List<String> searchDirs() {
		List<String> dirList = new ArrayList<String>();
		File dirFiles = new File(info.getWatcherDir());

		for (File d : dirFiles.listFiles()) {
			if (d.isDirectory()) {
				dirList.add("/" + d.getName());
			}
		}
		return dirList;
	}

	// 다건의 폴더들이 또는 파일을 찾을경우 가지고있을경우 사용
	public List<String> findToUploadFullDirPath(String providerPath) {
		String path = info.getWatcherDir() + providerPath;
		List<String> findDirPathList = new ArrayList<String>();

		List<String> searchDirPathArr = new ArrayList<String>();
		searchDirPathArr.add(path);

		while (true) {
			List<String> subDirArr = new ArrayList<String>();

			for (String dirPath : searchDirPathArr) {
				File findDir = new File(dirPath);

				for (File dir : findDir.listFiles()) {
					if (dir.isDirectory()) {
						subDirArr.add(dir.getPath());
					} else if (dir.isFile()) {
						// 중복된 폴더 제외하기위해 처리
						if (!findDirPathList.contains(dirPath)) {
							findDirPathList.add(dirPath);
						}
					} else {
						log.error("Invalid UploadPath" + dir.getPath());
					}
				}
			}

			// 추가적인 폴더가 있을경우 처리
			if (subDirArr.size() > 0) {
				searchDirPathArr.clear();
				for (String subDir : subDirArr) {
					searchDirPathArr.add(subDir);
				}
			} else {
				break;
			}
		}

		return findDirPathList;
	}

	/*
	 * 불특정 다수의 파일들을 삭제하기 위해 사용
	 */
	public void removeToIndividuaFile(String path, String snap_path, String compFilePath, List<String> msgList)
			throws Exception {
		if (path.length() > 0) {
			File f = new File(path);
			File cfp = new File(compFilePath);
			if (f.exists()) {
				f.delete();
				if (msgList != null) {
					log.debug(msgList.get(1) + path);
				} else {
					log.debug("Delete Original File : " + path);

				}
			}
//			else {
//				log.error(msgList.get(0) + path);
//			}

			if (cfp.exists()) {
				cfp.delete();
				if (msgList != null) {
					log.debug(msgList.get(1) + path);
				} else {
					log.debug("Delete Endfix Complete File : " + path);

				}
			}
//			else {
//				log.error(msgList.get(0) + path);
//			}
		}

		if (snap_path.length() > 0) {
			File sf = new File(path);
			if (sf.exists()) {
				sf.delete();
				if (msgList != null) {
					log.debug(msgList.get(1) + snap_path);
				} else {
					log.debug("Delete Snapshot File : " + path);

				}
			}
//			else {
//				log.error(msgList.get(0) + snap_path);
//			}
		}
	}

	private boolean isCompleteFile(String fileName) {
		String completeWork = fileName.substring(fileName.length() - 9, fileName.length());
		return completeWork.indexOf("_complete") == 0;
	}

	private String getUploadFileKey(String path, boolean isConsoleUpload) throws Exception {
		BufferedReader br = null;
		String uploadFileKey = null;
//		Gson gson = new Gson();
		try {
			if (isConsoleUpload) {
				String[] arr = path.split("/");
				String fileName = arr[arr.length -1];
				
				arr = fileName.split("\\.");
				uploadFileKey = arr[0];
								
				return uploadFileKey;			
			}
			br = new BufferedReader(new FileReader(path));
			while ((uploadFileKey = br.readLine()) != null) {
//			gson.fromJson(uploadFileKey, classOfT)
				String[] fileInfoline = uploadFileKey.split(":");
				if (fileInfoline[0].indexOf("k") > -1) {
					uploadFileKey = fileInfoline[1];
					break;
				}
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}

		return uploadFileKey;
	}

	protected void removeTimeOutFiles() throws Exception {
		List<String> dirList = searchDirs();
		List<File> storedFileList = new ArrayList<File>();

		Date pDate = new Date();
		pDate = new Date(pDate.getTime() + (1000 * 60 * 60 * 24 * -1));
		long pUnixDate = pDate.getTime();

		try {
			for (String dName : dirList) {
				String fileDirPath = info.getWatcherDir() + dName;

				File files = new File(fileDirPath);

				for (File f : files.listFiles()) {
					if (f.isDirectory()) {
						List<String> dirFullPathList = findToUploadFullDirPath(fileDirPath);
						// 파일이 있을경우 추가
						if (dirFullPathList.size() > 0) {
							for (String dirPath : dirFullPathList) {
								storedFileList.add(new File(dirPath));
							}
						}
//						if(dirFullPath.length() > 0) {
//							storedFileList.add(new File(dirFullPath));
//						}
					} else if (f.isFile()) {
						if (isCompleteFile(f.getName())) {
							storedFileList.add(new File(fileDirPath));
						}
					} else {
						log.error("Invalid UploadPath" + fileDirPath + "/" + f.getName());
					}
				}
			}

			for (File f : storedFileList) {
				if (f.isFile()) {
					long lastModifyDate = f.lastModified();
//					Date fDate = new Date(lastModifyDate);
//					String fStrDate = dateFormat.format(fDate);
//					fDate = dateFormat.parse(fStrDate);

//					int compareDate = pDate.compareTo(fDate);
					if (pUnixDate > lastModifyDate) {
						String filePath = f.getPath();

						File file = new File(filePath);

						if (file.delete()) {
							log.debug("removeTimeOutFiles : " + filePath);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * FTP로 업로드시 파일이 생성됬어도 중간에 쓰고있을수 있어서 문제가 발생되는 부분 처리 파일 크기와 최종 파일날짜가 일치하는지 확인후 진행
	 * 3분동안 시도하여 안될경우는 실패로 간주하고 파일 삭제
	 */
	public boolean isCompleteFileCopy(String filePath) {
		long lastSize = 0;
		long lastModifiy = 0;
		int completeCnt = 0;
		boolean isUploading = false;
		while (true) {
			try {
				File f = new File(filePath);
				if (f.exists()) {
					if (completeCnt > 3) {
						break;
					}

					if (lastSize == f.length() && lastModifiy == f.lastModified()) {
						completeCnt += 1;
						isUploading = false;
						log.info("Same :" + filePath + " : " + lastSize + " : " + lastModifiy);
					} else {
						lastSize = f.length();
						lastModifiy = f.lastModified();
						completeCnt = 0;
						if (!isUploading) {
							log.info(filePath + " : Writing....");
							isUploading = true;
						}
					}
				} else {
					return false;
				}
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return true;
	}

	public void removeFailedFiles(SendFileItemsDTO items) throws Exception {
		if (items.size() == 0) {
			return;
		}
		for (FileItemDTO item : items) {
			String completePath = item.getPhysicalPath() + "_complete";
			if (item.isConsoleUpload()) {
				completePath = "";
			}
			removeToIndividuaFile(item.getPhysicalPath(), item.getSnapshotPath(), completePath, null);
		}
	}

	// 폴더만 사용
//	public void consoleUploadWatch(String path)throws Exception  {
//		WatchService ws = null;
//		try {
//			 ws = FileSystems.getDefault().newWatchService();
//			 
//			 Path p = Paths.get(path);
//
//			 WatchKey watchKey = p.register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
//			 
//			 while (true) {
//				 for (WatchEvent<?> e : watchKey.pollEvents())
//		          {	
//		             System.out.println(e.kind() + ": "+ e.context()); 
//		          }
//				 
//				 if (!watchKey.reset()) {
//					 System.out.println("Watcher Finish");
//					 break;
//				 }
//			}
//		} finally {
//			ws.close();
//		}
//	}
}
