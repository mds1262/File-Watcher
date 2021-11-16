package net.catenoid.watcher.upload.config;

import static java.util.Collections.singletonMap;

import java.lang.reflect.Method;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

public interface MediaInfoDLL_Internal extends Library{
	String prodPath = "mediainfo";
	String localPath = "/usr/local/bin/mediainfo";
	public MediaInfoDLL_Internal INSTANCE = (MediaInfoDLL_Internal) Native.loadLibrary(
			prodPath, MediaInfoDLL_Internal.class,
			singletonMap(OPTION_FUNCTION_MAPPER, new FunctionMapper() {
				@Override
				public String getFunctionName(NativeLibrary lib, Method method) {
					// MediaInfo_New(), MediaInfo_Open() ...
					return "MediaInfo_" + method.getName();
				}
			}));

	// Constructor/Destructor
	Pointer New();

	void Delete(Pointer Handle);

	// File
	int Open(Pointer Handle, WString file);

	void Close(Pointer Handle);

	// Infos
	WString Inform(Pointer Handle);

	WString Get(Pointer Handle, int StreamKind, int StreamNumber, WString parameter, int infoKind, int searchKind);

	WString GetI(Pointer Handle, int StreamKind, int StreamNumber, int parameterIndex, int infoKind);

	int Count_Get(Pointer Handle, int StreamKind, int StreamNumber);

	// Options
	WString Option(Pointer Handle, WString option, WString value);
}
