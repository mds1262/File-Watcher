package net.catenoid.watcher.upload.config;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import net.catenoid.watcher.upload.dto.FileItemDTO;
import net.catenoid.watcher.upload.utils.CommonUtils;

public class LinuxLineParser implements LineParser {

	private static Logger log = Logger.getLogger(LinuxLineParser.class);
	private String strLastPath;
	private static SimpleDateFormat dateFmt;
	
	static {
		dateFmt = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
	}
	
	@Override
	public void parser(String watcher_dir, ArrayList<FileItemDTO> dirs, ArrayList<FileItemDTO> files, String line) {
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
					
					int pos3 = strFilename.indexOf("/");
					if(pos3 != -1) {
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
						dirs.add(CommonUtils.fromLineParser(watcher_dir, strFilename, lastModified, 0));
					}
				}
			} else if(line.charAt(0) == '-') {				
				String strFilename;
				String strDate;
				String strLength;
				
				int pos = line.lastIndexOf("\"");
				if(pos != -1) {
					pos = line.indexOf("\"");
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
					
					log.debug(watcher_dir + ", " + strFilename + ", " + lastModified + ", " + length);
					files.add(CommonUtils.fromLineParser(watcher_dir, strFilename, lastModified, length));
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
		return String.format("%s %s %s", lsPath, rootPath, lsFilePath);
	}
}
