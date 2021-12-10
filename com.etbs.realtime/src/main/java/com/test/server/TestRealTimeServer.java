package com.realtime.server;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TestRealTimeServer {

	private static final Logger log = LoggerFactory.getLogger(RealTimeServer.class);
	
	@Value("#{realtimeProp['server.port']}")
	private int _port;
	
	@Value("#{realtimeProp['profile.name']}")
	private String _profile;
	
	@Value("#{realtimeProp['test.telegram']}")
	private String telegram;
	
	private final static String _LAST_MODIFY_DATE = "2018-07-31 14:19";	
	
	public static Date LASTEXEC_DATETIME = null;
	
	private final static int _BODY_LENGTH  	    =  400;  // 전문바디부
	private final static int _DATA_LENGTH  		= 1284;  // 데이터부의 총 길이
	
	public static int THREAD_COUNT = 0;
	public static final int MAX_THREAD_COUNT = 10;
	
	public void run() { 
    	
    	try {
    		// 서버 시작 알림
    		log.debug(String.format("%s TestRealTimeServer Started : port = %s / %s", _profile, _port, _LAST_MODIFY_DATE));
    		            	    					
			// 전문부만 남기고 삭제
			telegram = telegram.substring(_DATA_LENGTH-_BODY_LENGTH);
			
    		// 전문을 파일 형태로 저장
			//FileLogUtil.write(telegram);
    		
			// 전문처리	    		
			setTelegram(telegram);
    	}
    	catch (Exception e) {
		}
    	
	}

	private void setTelegram(String telegram) throws InterruptedException {
		// 전문을 처리한다.	
		log.debug(String.format("THREAD_COUNT ---------------------------------------------------- %d", THREAD_COUNT));
		
		if(THREAD_COUNT <= MAX_THREAD_COUNT) {
			// 쓰레드의 수가 최대 수보다 작으면 쓰레드 생성
			Thread socketThread = new Thread(new SocketThread(telegram));
			socketThread.start();
		}
		else {
			Thread.sleep(1000);	
			setTelegram(telegram);
		}
	}
}
