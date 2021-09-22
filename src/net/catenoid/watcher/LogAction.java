package net.catenoid.watcher;

public interface LogAction { 
	public static final String PROGRAM_START 		= "[PRG_START]";
	public static final String PROGRAM_VERSION 		= "[PRG_VER]";
	public static final String PROGRAM_CONFIG		= "[PRG_CONF]";
	public static final String HTTP_SERVER_START	= "[HTTP_START]";
	public static final String HTTP_LOG 			= "[HTTP_LOG]";
	public static final String JSON_FORMAT_ERROR	= "[JSON_ERR]";
	public static final String CONFIG_ERROR			= "[CONF_ERR]";
	public static final String MEDIAINFO_ERROR		= "[MEDIAINFO_ERR]";
	public static final String THUMBANIL_CHECK		= "[THUMB_CHECK]";
	public static final String SH_STDOUT			= "[SH_STDOUT]";
	public static final String COMMAND				= "[SH_COMMAND]";
}

