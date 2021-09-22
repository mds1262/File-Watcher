package net.catenoid.watcher.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

public class HttpAgent {
	
	private static volatile ThreadSafeClientConnManager threadSafeClientConnManager=null;
	//private static HttpClient httpClient = null;
	private  HttpClient httpClient = null;
	public HttpClient newHttpClient() {
		
		if(httpClient == null)
		{
			 ThreadSafeClientConnManager cm =getInstance();
	        
            httpClient = new DefaultHttpClient( cm );
	        
            //httpClient = new DefaultHttpClient();
			 httpClient = WebClientDevWrapper.wrapClient(httpClient);
		}
		
		return httpClient;
	}
	
	
	public static ThreadSafeClientConnManager getInstance(){
		
		if(threadSafeClientConnManager==null){
			synchronized(ThreadSafeClientConnManager.class){	
				if(threadSafeClientConnManager==null){
					threadSafeClientConnManager=new ThreadSafeClientConnManager();
					threadSafeClientConnManager.setDefaultMaxPerRoute( 10 );
					threadSafeClientConnManager.setMaxTotal( 100 );
				}
			}
		} 
		return threadSafeClientConnManager;
	}
	
	public void printCookie() {
		List<Cookie> cookies = ((DefaultHttpClient)httpClient).getCookieStore().getCookies();

        if (!cookies.isEmpty()) {
        	for (int i = 0; i < cookies.size(); i++) {
        		String cookieString = cookies.get(i).getName() + "=" + cookies.get(i).getValue();
        		System.out.println(cookieString);
        	}
        }
	}
	
	public static String getContentCharSet(String defaultCharSet, final HttpEntity entity)  {

		if (entity == null) 
		{ 
			return defaultCharSet;
			//throw new IllegalArgumentException("HTTP entity may not be null"); 
		}
		
		String charset = null;

		if (entity.getContentType() != null) {

			HeaderElement values[] = entity.getContentType().getElements();
	
			if (values.length > 0) {	
				NameValuePair param = values[0].getParameterByName("charset");		
				if (param != null) {
					charset = param.getValue();
				}
			}
		}
		
		if(charset == null) charset = defaultCharSet;
		
		return charset;
	}
	
	public static String generateString(InputStream stream, String charset) {
		
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(stream, charset);
		} catch (UnsupportedEncodingException e1) {
			reader = new InputStreamReader(stream);
		}
		
		BufferedReader buffer = new BufferedReader(reader);
		StringBuilder sb = new StringBuilder();

		try {
			
			String cur;
			while ((cur = buffer.readLine()) != null) {
				sb.append(cur + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		  
		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
}
