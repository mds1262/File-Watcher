package net.catenoid.watcher.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DBUtils {

	private static Logger log=LoggerFactory.getLogger(H2DBUtils.class);
	private static final int H2_TRACE_LEVEL = 0;
	public static Connection connectDatabase(String pathName) {		
		
		String dbPath = String.format("%s/%s", System.getProperty("user.dir"), pathName);
		String dbUrl_string = String.format("jdbc:h2:file:%s/MEDIAWATCHER;TRACE_LEVEL_FILE=%d;LOCK_MODE=0", dbPath, H2_TRACE_LEVEL);
		//String dbUrl_string = String.format("jdbc:h2:tcp://localhost/%s/MEDIAWATCHER;TRACE_LEVEL_FILE=%d;LOCK_MODE=0", dbPath, H2_TRACE_LEVEL);
    	Connection conn = null;
    	
		try	{
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection(dbUrl_string); 
		}catch (Exception e) {
		    log.error(e.getMessage());
		}
		
		return conn;
	}
	public static void close(Connection conn,Statement stmt,ResultSet rs){
		
		if(rs !=null)		try {	rs.close();} catch (SQLException e) {log.error(e.getMessage());}
		if(stmt !=null)  try {	stmt.close();	} catch (SQLException e) {	log.error(e.getMessage());}
		if(conn !=null)  try {	conn.close();	} catch (SQLException e) {	log.error(e.getMessage());}
	}
	public static void close(Connection conn,PreparedStatement pstmt,ResultSet rs){
		
		if(rs !=null)		try {	rs.close();} catch (SQLException e) {log.error(e.getMessage());}
		if(pstmt !=null)	try {	pstmt.close();} catch (SQLException e) {log.error(e.getMessage());}
		if(conn !=null)  try {	conn.close();	} catch (SQLException e) {	log.error(e.getMessage());}
		
	}
}
