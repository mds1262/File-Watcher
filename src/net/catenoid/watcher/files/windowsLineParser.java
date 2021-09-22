package net.catenoid.watcher.files;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class windowsLineParser implements LineParser {
	
	private String strLastPath;
	private static SimpleDateFormat dateFmt;
	
	static {
		dateFmt = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
	}
	
	@Override
	public void parser(String watcher_dir, ArrayList<FileItem> dirs, ArrayList<FileItem> files, String line) {
		watcher_dir = watcher_dir.replace("\\", "/");
		if(line.length() > 0) {			
			if(line.charAt(0) == '\"') {
				// directory
				String strFilename;
				strFilename = line.substring(1, line.length()-2);
				strFilename = strFilename.replace("\\:", ":");
				strFilename = strFilename.replace("\\", "/");
				strLastPath = strFilename;
			} else if(line.charAt(0) == 't') {
				// total...
			} else if(line.charAt(0) == 'd') {
				String strFilename;
				String strDate;
				
				int pos = line.lastIndexOf("\"");
				if(pos != -1) {
					
					pos = line.lastIndexOf("\"", pos-1);
					strFilename = line.substring(pos+1, line.length()-1);
					strFilename = strFilename.replace("\\", "/");
					
					String tempDir = strLastPath.replace(watcher_dir, "");
					if(tempDir.length() > 0) {
						strFilename = strLastPath + "/" + strFilename;						
						int pos2 = pos-1;
						pos = line.lastIndexOf(" ", pos2);
						while(pos2 == pos) {
							pos2 = pos-1;
							pos = line.lastIndexOf(" ", pos2);
						}
						strDate = line.substring(pos+1, pos2+1);

						String strDateLong = ""; 
						try {
							Date d = dateFmt.parse(strDate);
							strDateLong = "" + d.getTime();
						} catch (ParseException e) {
							e.printStackTrace();
						}
						long lastModified = Long.parseLong(strDateLong);						
						dirs.add(FileItem.fromLineParser(watcher_dir, strFilename, lastModified, 0));
					}
				}
			} else if(line.charAt(0) == '-') {				
				String strFilename;
				String strDate;
				String strLength;
				
				int pos = line.lastIndexOf("\"");
				if(pos != -1) {
					pos = line.lastIndexOf("\"", pos-1);
					strFilename = line.substring(pos+1, line.length()-1);
					strFilename = strLastPath + "/" + strFilename;
					
					int pos2 = pos-1;
					pos = line.lastIndexOf(" ", pos2);
					while(pos2 == pos) {
						pos2 = pos-1;
						pos = line.lastIndexOf(" ", pos2);
					}
					strDate = line.substring(pos+1, pos2+1);
					
					String strDateLong = ""; 
					try {
						Date d = dateFmt.parse(strDate);
						strDateLong = "" + d.getTime();
					} catch (ParseException e) {
						e.printStackTrace();
					}
					long lastModified = Long.parseLong(strDateLong);

					pos2 = pos-1;
					pos = line.lastIndexOf(" ", pos2);
					
					strLength = line.substring(pos+1, pos2+1);
					long length = Long.parseLong(strLength);
					files.add(FileItem.fromLineParser(watcher_dir, strFilename, lastModified, length));
				}
			}
		}
	}

	@Override
	public void clear() {
		strLastPath = "";		
	}

	@Override
	public String getOption(String lsPath, String rootPath, String lsFilePath) {
		return String.format("cmd /c %s %s -R -l --time-style=\"+%%Y-%%m-%%d_%%H:%%M:%%S\" -Q -G > %s", lsPath, rootPath, lsFilePath);
	}

}
