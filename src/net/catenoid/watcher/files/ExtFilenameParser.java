package net.catenoid.watcher.files;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.kollus.utils.myUtils;

public class ExtFilenameParser {

	private static Logger log = Logger.getLogger(ExtFilenameParser.class);
	private final char extensionSeparator = '.';
	private static final int MAX_CATEGORY = 3;
	
	/**
	 * 모든 예약된 폴더명을 등록합니다.
	 */
	private static String[] expand_folder_reserved_strings = new String[] {
		"_encrypt",
		"_passthrough",
		"_passthrough_encrypt",
		"_audio",
		"_audio_encrypt",
		"_audio_passthrough",
		"_audio_passthrough_encrypt",
		"_upload_file_key",
		"_media_content_key",
	};
	
	/**
	 * 암호화 폴더를 등록합니다.
	 */
	private static String[] expand_folder_encypt_strings = new String[] {
		"_encrypt",
		"_passthrough_encrypt",
		"_audio_encrypt",
		"_audio_passthrough_encrypt",
	};

	/**
	 * audio 폴더를 등록합니다.
	 */
	private static String[] expand_folder_audio_strings = new String[] {
		"_audio",
		"_audio_encrypt",
		"_audio_passthrough",
		"_audio_passthrough_encrypt",
	};

	/**
	 * passthrough 폴더를 등록합니다.
	 */
	private static String[] expand_folder_passthrough_strings = new String[] {
		"_passthrough",
		"_passthrough_encrypt",
		"_audio_passthrough",
		"_audio_passthrough_encrypt",
	};
	
	/**
	 * upload_file_key 폴더를 등록합니다.
	 */
	private static String[] expand_folder_upload_file_key_strings = new String[] {
		"_upload_file_key",
	};

	/**
	 * media_content_key 폴더를 등록합니다.
	 */
	private static String[] expand_folder_media_content_key_strings = new String[] {
		"_media_content_key",
	};

	/**
	 * 파일 경로 분석을 위해 입력한 전체 경로
	 */
	private String fullPath;

	/**
	 * _(underscore)로 시작되는 카테고리 정보
	 * 데이터에는 _ (underscore)가 포함되지 않는다. 
	 * 샘플 : 수학/김선생/test
	 */
	private String strCategory;
	
	/**
	 * 확장자를 포함한 파일명
	 */
	private String strFilename;
	
	/**
	 * 확장자를 제거한 파일명을 제목으로 사용합니다.
	 */
	private String strTitle;
	
	/**
	 * 확장자 .(dot)은 제외
	 */
	private String strExtension;
	
	/**
	 * _passthrough_encrypt와 같은 폴더 옵션을 제거한 폴더명으로 변환된 경로 정보
	 */
	private String strUploadPath;
	
	/**
	 * media_profile_key 파일명에 포함된 미디어 프로파일명
	 * passthrough일때만 분석이 됩니다.
	 */
	private String strMediaProfileKey = "";
	
	/**
	 * passthrough 업로드를 확인할 수 있습니다.
	 */
	private boolean passthroughFolder = false;
	
	/**
	 * 암호화 대상 폴더 인지 확인할 수 있습니다.
	 */
	private boolean encryptFolder = false;
	
	/**
	 * Audio 폴더인지 확인할 수 있습니다.
	 */
	private boolean audioFolder = false;
	
	/**
	 * content_provider_key
	 */
	private String strContentProviderKey;
	
	/**
	 * parser 동작중 오류 상황이 발생한 경우 true
	 */
	private boolean _error = false;
	
	/**
	 * upload_file_key 경로로 업로드된 경우 true
	 */
	private boolean uploadFileKeyFolder = false;
	
	/**
	 * media_content_key 경로로 업로드된 경우 true
	 */
	private boolean mediaContentKeyFolder = false;

	private ExtFilenameParser() {
	}
	
	public static ExtFilenameResult Parser(String path) {
		ExtFilenameParser parser = new ExtFilenameParser();
		parser.parser(path);	
		
		ExtFilenameResult result = new ExtFilenameResult(parser);
		return result;
	}
	
	private ArrayList<String> splitPath(String upload_path) {
		String[] paths = upload_path.split("/");
		return new ArrayList<String>(Arrays.asList(paths));
	}

	private void parser(String str) {
		fullPath = str;
		ArrayList<String> paths = splitPath(fullPath);
				
		//  서비스어카운트만 있는 경우 size = 2
		if(paths.size() <= 2) {
//			throw new FilenameParserException("not exist filename: " + fullPath);
			log.error("not exist filename: " + fullPath);
			_error = true;
			return;
		}
		
		strContentProviderKey   = paths.get(1);
		
		boolean isReservedPath = isContainsFolder(expand_folder_reserved_strings, paths.get(2));

		/**
		 * 카테고리 정보를 찾습니다.
		 */
		
		/**
		 * paththrough와 같은 예약 폴더를 포함하는 경우 찾는 인덱스의 시작을 3에서 시작합니다.
		 */
		int idx_category = isReservedPath ? 3 : 2;
		int idx_last = paths.size() -1;
		int idx_start = idx_category;

		StringBuilder sbCategory = new StringBuilder();
		int categoty_count = 0;
		for(int i = idx_category; i < idx_last; i++) {
			if(isCategory(paths.get(i))) {
				categoty_count++;
				if(categoty_count > MAX_CATEGORY) {
					idx_start = i;
					break;
				}
				if(sbCategory.length() > 0) {
					sbCategory.append("/");
				}
				sbCategory.append(paths.get(i).substring(1));
				idx_start++;
			} else {
				idx_start = i;
				break;
			}
		}
		strCategory = sbCategory.toString();
		
		/**
		 * 카테고리 정보를 제외한 나머지 경로를 filename으로 설정합니다.
		 */
		StringBuilder sbPath = new StringBuilder();
		for(int i = idx_start; i < paths.size(); i++) {
			if(sbPath.length() > 0) {
				sbPath.append("/");
			}
			sbPath.append(paths.get(i));
		}
		String strPath = sbPath.toString();		
		strFilename = strPath;
		
		/**
		 * 확장자 정보를 확인합니다. .(dot)을 제외한 확장자를 얻습니다.
		 */
		int dot = strFilename.lastIndexOf(extensionSeparator );
		strExtension = dot == -1 ? "" : strFilename.substring(dot + 1);
		
		/**
		 * filename에서 확장자를 제외한 부분을 title로 사용합니다.
		 */
		strTitle = dot != -1 ? strFilename.substring(0, dot) : strFilename;

		/**
		 * 예약된 폴더 정보를 가지고 있다면 strMediaProfileKey를 분석한다.
		 */
		if(isReservedPath) {
			/**
			 * passthrough이면 encoding_profile_key를 추가 분석한다.
			 */
			if(checkPassThroughFolder(paths.get(2))) {
				setPassthrough(true);
				int sep = strFilename.lastIndexOf('_');
				if(sep == -1) {
					strMediaProfileKey = "";
					sep = strFilename.length();
				} else {
					strMediaProfileKey = dot != -1 ? strFilename.substring(sep + 1, dot) : strFilename.substring(sep + 1);
					strTitle = strTitle.substring(0, strTitle.length() - (strMediaProfileKey.length()+1));
				}
			}
			
			setAudioFolder(checkAudioFolder(paths.get(2)));
			setEncryptPath(checkEncryptFolder(paths.get(2)));
			setUploadFileKeyFolder(checkUploadFileKeyFolder(paths.get(2)));
			setMediaContentKeyFolder(checkMediaContentKeyFolder(paths.get(2)));

			// paths 배열의 수정은 마지막에 해야한다.
			paths.remove(2);
		}
		
		/**
		 * upload_path에는 passthrough등 예약가 포함된 2번째 경로를 제거하고 등록된다.
		 */
		strUploadPath = myUtils.implodeList(paths, "/");
	}
	
	private void setMediaContentKeyFolder(boolean checkMediaContentKeyFolder) {
		mediaContentKeyFolder   = checkMediaContentKeyFolder;
	}

	private void setUploadFileKeyFolder(boolean checkUploadFileKeyFolder) {
		uploadFileKeyFolder  = checkUploadFileKeyFolder;
	}

	/**
	 * strings에 s가 포함되는 경우 성공(true)를 반환합니다.
	 * @param strings
	 * @param s
	 * @return
	 */
	private boolean isContainsFolder(final String[] strings, String s) {
		for(String item : strings) {
			if(item.compareTo(s) == 0) {
				return true;
			}
		}
		return false;
	}

	private boolean isCategory(String s) {
		return s.startsWith("_");
	}
	
	public String category() {
		return strCategory;
	}

	public String extension() {
		return strExtension;
	}
	
	/**
	 * 확장자 제외, _sistar_tropian-mobile2-high.mp4 의 경우 profile명 제외<br>
	 * media_profile_key, category를 제거한 이름을 title로 사용한다.
	 * @return
	 */
	public String title() {
		return strTitle;
	}
	
	/**
	 * 파일명은 경로와 확장자를 포함합니다.
	 * /tropian/_passthrough_encrypt/_수학/김선생/sistar_tropian-mobile2-high.mp4 의 경우
	 * 김선생/sistar_tropian-mobile2-high.mp4 입니다.
	 * @return
	 */
	public String filename() {
		return strFilename;
	}

	/**
	 * _passthrough_encrypt와 같은 폴더 옵션을 제거한 폴더명으로 변환된 경로 정보
	 * _passthrough,_passthrough_enc를 제거한 upload_path를 전송한다.
	 * 0 : content_provider_key
	 * 1 : _passthrough or _passthrough_enc
	 * 2,3 : category
	 * 4 : path
	 * 5 : file  {filename}_{media_profile_key}.{ext}
	 */
	public String upload_path() {
		return strUploadPath;
	}

	/**
	 * media_profile_key가  zero-string인 경우 null을 반환하도록 한다.
	 * @return
	 */
	public String media_profile_key() {
		if(myUtils.isEmpty(strMediaProfileKey)) {
			return null;
		}
		return strMediaProfileKey;
	}

	public void debugPrint() {
		log.debug("-----------------------------------------------------------------------");
		log.debug("content_provider_key: " + strContentProviderKey);
		log.debug("fullPath: " + fullPath); 
		log.debug("filename: " + strFilename);
		log.debug("ext: " + strExtension);
		log.debug("title: " + strTitle);
		log.debug("uploadPath: " + strUploadPath);
		log.debug("category : " + strCategory);
		if(strMediaProfileKey.length() > 0) {
			log.debug("prfoile : " + strMediaProfileKey);
		}
	}

	public void debugSystemout() {
		log("-----------------------------------------------------------------------");
		log("content_provider_key: " + strContentProviderKey);
		log("fullPath: " + fullPath); 
		log("filename: " + strFilename);
		log("ext: " + strExtension);
		log("title: " + strTitle);
		log("uploadPath: " + strUploadPath);
		log("category : " + strCategory);
		if(strMediaProfileKey.length() > 0) {
			log("prfoile : " + strMediaProfileKey);
		}
	}

	private static void log(String s) {
		System.out.println(s);
	}

	private boolean checkEncryptFolder(String path) {
		return isContainsFolder(expand_folder_encypt_strings, path);
	}
	
	private boolean checkAudioFolder(String path) {
		return isContainsFolder(expand_folder_audio_strings, path);
	}
	
	private boolean checkPassThroughFolder(String path) {
		return isContainsFolder(expand_folder_passthrough_strings, path);
	}
	
	private boolean checkUploadFileKeyFolder(String path) {
		return isContainsFolder(expand_folder_upload_file_key_strings, path);
	}
	
	private boolean checkMediaContentKeyFolder(String path) {
		return isContainsFolder(expand_folder_media_content_key_strings, path);
	}

	/**
	 * 파일 경로 분석을 위해 입력한 전체 경로
	 * @return
	 */
	public String fullpath() {
		return this.fullPath;
	}

	/**
	 * passthrough 경로로 업로드 되었는지 확인합니다.
	 * _passthrough / _passthrough_encrypt / _audio_passthrough / _audio_passthrough_encrypt ... 경로로 업로드 된 경우 true입니다. 
	 * @return
	 */
	public boolean isPassthrough() {
		return passthroughFolder;
	}
	
	/**
	 * passthrough 경로로 업로드 되었는지 설정합니다.
	 * @param flag
	 */
	private void setPassthrough(boolean flag) {
		passthroughFolder = flag;
	}

	public boolean isAudioPath() {
		return audioFolder;
	}
	
	private void setAudioFolder(boolean flag) {
		audioFolder = flag;
	}
	
	public String content_provider_key() {
		return strContentProviderKey;
	}

	private void setEncryptPath(boolean checkEncrypt) {
		encryptFolder  = checkEncrypt;
	}

	public boolean isEncryptPath() {
		return encryptFolder;
	}

	public boolean isError() {
		return _error;
	}

	public boolean isUploadFileKeyFolder() {
		return uploadFileKeyFolder;
	}

	public boolean isMediaContentKeyFolder() {
		return mediaContentKeyFolder;
	}
}

class ExtFilenameResult {
	
	private String category;
	private String filename;
	private String fullpath;
	private String uploadPath;
	private String title;
	private String contentProviderKey;
	private String extension;
	private String mediaProfileKey;
	private boolean audioPath;
	private boolean encryptPath;
	private boolean passthroughPath;
	private boolean uploadFileKeyPath;
	private boolean mediaContentKeyPath;
	private boolean error;
	

	public ExtFilenameResult(ExtFilenameParser p) {
		this.category = p.category();
		this.filename = p.filename();
		this.fullpath = p.fullpath();
		this.uploadPath = p.upload_path();
		this.title = p.title();
		this.contentProviderKey = p.content_provider_key();
		this.extension = p.extension();
		this.mediaProfileKey = p.media_profile_key();
		this.audioPath = p.isAudioPath();
		this.encryptPath = p.isEncryptPath();
		this.passthroughPath = p.isPassthrough();
		this.uploadFileKeyPath = p.isUploadFileKeyFolder();
		this.mediaContentKeyPath = p.isMediaContentKeyFolder();
		this.error = p.isError();
	}


	public String getCategory() {
		return category;
	}


	public String getFilename() {
		return filename;
	}


	public String getFullpath() {
		return fullpath;
	}


	public String getUploadPath() {
		return uploadPath;
	}


	public String getTitle() {
		return title;
	}


	public String getContentProviderKey() {
		return contentProviderKey;
	}


	public String getExtension() {
		return extension;
	}


	public String getMediaProfileKey() {
		return mediaProfileKey;
	}


	public boolean isAudioPath() {
		return audioPath;
	}


	public boolean isEncryptPath() {
		return encryptPath;
	}


	public boolean isPassthroughPath() {
		return passthroughPath;
	}


	public boolean isUploadFileKeyPath() {
		return uploadFileKeyPath;
	}


	public boolean isMediaContentKeyPath() {
		return mediaContentKeyPath;
	}


	public boolean isError() {
		return error;
	}	
	
}