package net.catenoid.watcher.files;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.kollus.json_data.BaseCommand;

public class SendFileItems extends ArrayList<FileItem> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9066947625019002093L;
	
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(SendFileItems.class);	

	public String toString(int watcher_file_kind) {
		for(FileItem item : this) {
			item.set_watcher_file_kind(watcher_file_kind);
		}
		Gson gson = BaseCommand.gson(false);
		return gson.toJson(this);
	}
	
	public String toString(String module_key, int watcher_file_kind) {
		for(FileItem item : this) {
			item.set_module_key(module_key);
			item.set_watcher_file_kind(watcher_file_kind);
		}
		Gson gson = BaseCommand.gson(false);
		return gson.toJson(this);
	}

	/**
	 * item의 pysical_path가 등록한 객체의 key, content_path, snapshot_path를 업데이트 한다.
	 * @param item
	 */
	public void update(FileItem item) {
		for(FileItem f : this) {
			if(f.get_physical_path().compareTo(item.get_physical_path()) == 0) {
				f.set_upload_file_key(item.get_upload_file_key());
				f.set_content_path(item.get_content_path());
				f.set_snapshot_path(item.get_snapshot_path());
				f.setPoster(item.getPoster());
				f.set_checksum_type(item.get_checksum_type());
				break;
			}
		}
	}
}
