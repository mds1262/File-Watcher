package net.catenoid.watcher.uploadImp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.kollus.json_data.BaseCommand;

import net.catenoid.watcher.config.Config;
import net.catenoid.watcher.config.WatcherFolder;
import net.catenoid.watcher.upload.KusUploadService;
import net.catenoid.watcher.upload.dto.KollusApiWatchersDTO;
import net.catenoid.watcher.upload.dto.KollusApiWatcherContentDTO;
import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.upload.dto.SendFileItemsDTO;
import net.catenoid.watcher.upload.utils.KusUploadUtils;

/**
 * 
 * @author MDS
 * @category Kus Http Upload Module
 * @since 2020 09 16 KUS로 업로드된 파일 감지하기위한 모듈
 */

public class KusUploadServiceImp implements KusUploadService {
	private static Logger log = Logger.getLogger(KusUploadServiceImp.class);

	private List<String> removeFiles = null;
	private SendFileItemsDTO fileList = null;
	private List<String> dirList = null;

	protected KusUploadUtils utils = null;

	public KusUploadServiceImp(WatcherFolder info, Config conf) {
		// TODO Auto-generated method stub

		utils = new KusUploadUtils(info, conf);
	}

	@Override
	public void findWorkFileList() throws Exception {
		// TODO Auto-generated method stub
		this.removeFiles = new ArrayList<String>();
		this.fileList = new SendFileItemsDTO();
		this.dirList = new ArrayList<String>();

		readFiles(fileList, removeFiles, dirList);
		
		if(fileList.size() == 0) {
			return;
		}
		sendToHttpApi(fileList, "register");
	}

	@Override
	public void moveToWorkFiles() throws Exception {
		// TODO Auto-generated method stub

		if(fileList.size() == 0) {
			return;
		}
		
		utils.createSnapFile(fileList);

		if(fileList.size() == 0) {
			return;
		}
		
		utils.moveToWorkDir(fileList);
	}

	@Override
	public int sendCompleteWorkFiles() throws Exception {
		int cnt = 0;
		
		if(fileList.size() == 0) {
			return cnt;
		}
		
		String responseBody = utils.sendPostToApi(fileList, "copy");

		if (responseBody == null) {
			return cnt;
		}

		Gson gson = BaseCommand.gson(false);
		KollusApiWatchersDTO apiResult = gson.fromJson(responseBody, KollusApiWatchersDTO.class);

		if (apiResult.error != 0) {
			log.error(responseBody);
			utils.failApiResultOrRegisterProcess(apiResult, null);
			return cnt;
		}

		for (KollusApiWatcherContentDTO item : apiResult.result.watcher_files) {
			if (item.error == 0) {
				// error가 아닌 경우만 media_content_id가 있으나 사용처가 없어 삭제함
				FileItemDTO findItem = utils.findSendItem(fileList, item.result.key);
				if (findItem == null) {
					log.error("FileItem  파일 정보를 찾을 수 없습니다. [" + item.result.key + "]");

				}
				cnt += 1;
				continue;
			}

			log.error("error code: " + item.error + ", error message: " + item.message);

			if (item.result == null) {
				continue;
			}

			FileItemDTO findItem = utils.findSendItem(fileList, item.result.key);

			if (findItem == null) {
				log.error("실패한 파일 정보를 찾을 수 없습니다. [" + item.result.key + "]");
				continue;
			}
			List<String> msgList = new ArrayList<String>();

			msgList.add("FAIL");
			msgList.add("Fail to API transfered 삭제 성공" + findItem.getPhysicalPath());
			msgList.add("Fail to API transfered 삭제 실패" + findItem.getPhysicalPath());

			String completePath = findItem.getPhysicalPath() + "_complete";
			if (findItem.isConsoleUpload()) {
				completePath = "";
			}
			
			log.warn("Complete Api is deleted to status is failed" + findItem.toString());
			
			utils.removeToIndividuaFile(findItem.getPhysicalPath(), findItem.getSnapshotPath(), completePath, msgList);
			
			findItem.setCompleteFail(true);
		}

		return cnt;
	}

	// http_upload 폴더를 읽어 이동할 파일을 선
	private void readFiles(SendFileItemsDTO fileList, List<String> removeFiles, List<String> dirList) throws Exception {
		dirList = utils.searchDirs();

		List<File> storedFileList = new ArrayList<File>();

		// provieder Key명으로 된 폴더들의 모든경로를 조회
		for (String dir : dirList) {
			List<String> dirFullPathList = utils.findToUploadFullDirPath(dir);

			// 파일이 있을경우 추가
			if (dirFullPathList.size() > 0) {
				for (String dirPath : dirFullPathList) {
					storedFileList.add(new File(dirPath));
				}
			}
		}

		// 조회된 폴더 경로로 파일 조회
		for (File f : storedFileList) {
			utils.getUploadFullFilePath(f.listFiles(), fileList, removeFiles);
		}
	}

	/*
	 * 파일 이동이 완료된 파일들을 API로 전송 후 기존 해당 관련 파일들을 삭제 삭제되는 경우는 API전송이 원할하지 않아 전송이 안될경우에만
	 * 삭제가 된다
	 */
	public void sendToHttpApi(SendFileItemsDTO items, String apiType) throws Exception {
		String responseBody = utils.sendPostToApi(items, apiType);
		log.info(responseBody);

		if (responseBody == null) {
			utils.removeFailedFiles(items);
			return;
		}

		Gson gson = BaseCommand.gson(false);
		KollusApiWatchersDTO apiResult = gson.fromJson(responseBody, KollusApiWatchersDTO.class);

		if (apiResult.error != 0) {
			utils.failApiResultOrRegisterProcess(apiResult, null);
			return;
		}

		for (KollusApiWatcherContentDTO item : apiResult.result.watcher_files) {
			if (item.error == 0) {
				/**
				 * error == 0인 등록에 성공한 파일
				 */
				FileItemDTO f = utils.convertResultApiItem(item);
				items.update(f);
				continue;
			}

			log.error(item.message);

			utils.failApiResultOrRegisterProcess(null, item);
		}
	}
}
