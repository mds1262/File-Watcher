package net.catenoid.watcher.files;

import java.util.ArrayList;

public interface LineParser {
	void parser(String watcher_dir, ArrayList<FileItem> dirs, ArrayList<FileItem> files, String line);
	void clear();
	String getOption(String lsPath, String rootPath, String lsFilePath);
}
