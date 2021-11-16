package net.catenoid.watcher.upload.dto;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.kollus.json_data.BaseCommand;

public class SendFileItemsDTO extends ArrayList<FileItemDTO> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9066947625019002093L;
	
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(SendFileItemsDTO.class);	

	public String toString(int watcher_file_kind) {
		for(FileItemDTO item : this) {
			item.setWatcherFileKind(watcher_file_kind);
		}
		Gson gson = BaseCommand.gson(false);
		return gson.toJson(this);
	}
	
	public String toString(String module_key, int watcher_file_kind) {
		for(FileItemDTO item : this) {
			item.setModuleKey(module_key);
			item.setWatcherFileKind(watcher_file_kind);
		}
		Gson gson = BaseCommand.gson(false);
		return gson.toJson(this);
	}

	/**
	 * item의 pysical_path가 등록한 객체의 key, content_path, snapshot_path를 업데이트 한다.
	 * @param item
	 */
	public void update(FileItemDTO item) {
		for(FileItemDTO f : this) {
			if(f.getPhysicalPath().compareTo(item.getPhysicalPath()) == 0) {
				f.setUploadFileKey(item.getUploadFileKey());
				f.setContentPath(item.getContentPath());
				f.setSnapshotPath(item.getSnapshotPath());
				f.setPoster(item.getPoster());
				f.setChecksumType(item.getChecksumType());
				break;
			}
		}
	}
}
