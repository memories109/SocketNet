package com.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.realtime.server.*;

public class App 
{	
	public static ClassPathXmlApplicationContext context;
	
	private static final Logger log = LoggerFactory.getLogger(App.class);
	
    public static void main( String[] args )
    {	
    	// 현재 카카오톡 수신자는 이도한으로 고정되어 있음
    	context = new org.springframework.context.support.ClassPathXmlApplicationContext("config/app.xml");
    	
    	boolean isDev = System.getProperty("spring.profiles.active") == null||"local".equals(System.getProperty("spring.profiles.active"))||"dev".equals(System.getProperty("spring.profiles.active"));
    	
    	if(isDev == true) {    		
    		log.debug("IS DEV START ------------------------------------------------------------------------------");
        	//TestRealTimeServer server = context.getBean(TestRealTimeServer.class);
        	RealTimeServer server = context.getBean(RealTimeServer.class);
    		server.run();
    	}
    	else {
    		log.debug("IS REAL START -----------------------------------------------------------------------------");
    		RealTimeServer server = context.getBean(RealTimeServer.class);        	
    		server.run();	
    	}
    }
}
