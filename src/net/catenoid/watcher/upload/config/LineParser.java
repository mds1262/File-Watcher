package net.catenoid.watcher.upload.config;

import java.util.ArrayList;

import net.catenoid.watcher.upload.dto.FileItemDTO;

public interface LineParser {
	void parser(String watcher_dir, ArrayList<FileItemDTO> dirs, ArrayList<FileItemDTO> files, String line);
	void clear();
	String getOption(String lsPath, String rootPath, String lsFilePath);
}
