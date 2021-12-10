package com.realtime.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import com.realtime.Constants;
import com.realtime.util.FileLogUtil;
import com.realtime.util.StringUtil;


@Component
public class RealTimeServer {

	private static final Logger log = LoggerFactory.getLogger(RealTimeServer.class);
	
	@Value("#{realtimeProp['server.port']}")
	private int _port;
	
	@Value("#{realtimeProp['profile.name']}")
	private String _profile;
	
	@Value("#{realtimeProp['command.file']}")
	private String _basename;	
	
	private ReloadableResourceBundleMessageSource messageSource;			// 2018.12
	
	private static ExecutorService pool = new ThreadPoolExecutor( 10, 10, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100, true ) );		// 스레드 풀 추가. 2018.12
	public static volatile List<String> mbrNoList = new ArrayList<>();					// 처리중 mbrNo 목록
	public static volatile Map<String, Runnable> collisionMap = new Hashtable<>();		// mbrNo 충돌로 대기중인 task 맵
	
	private final static String _LAST_MODIFY_DATE = "2018-08-30 13:36";	
	public static Date LASTEXEC_DATETIME = null;	
	
	private final static int _BODY_LENGTH  	    =  400;  // 데이터바디부
	private final static int _DATA_LENGTH  		= 1284;  // 데이터부의 총 길이
	
	public static int THREAD_COUNT = 0;
	public static final int MAX_THREAD_COUNT = 10;
	private static Object cntObj = new Object();
	
	private String state = "run";
	
	private ServerSocket serverSocket = null;
	private InputStream in = null;
	private BufferedReader br = null;
	private Socket socket = null;
	private StringBuffer sbRead = null;
	
	public void run() {
		
    	// shutdown command 모니터
		CommandMonitor monitor = new CommandMonitor();
		monitor.start();
    	
		// mbrNo 충돌 처리 스레드
		Runnable retryer = new Retryer();
		Thread t = new Thread(retryer);
		t.start();


    	try { 
			serverSocket = new ServerSocket(_port); 

			// 서버 시작 알림
			log.debug(String.format("%s RealTimeServer Started : port = %s / %s", _profile, _port, _LAST_MODIFY_DATE));
    		
    		while(true) {
    		
    			// 연결대기
    			socket = serverSocket.accept();
	    			
	    		// 접속 연결 시간 저장
	    		LASTEXEC_DATETIME = new Date();
        		
        		log.debug("");
	    		log.debug("=========================================================");
	    		log.debug("Socket connection has established...... ");
	    		log.debug("=========================================================");
	    		log.debug("");


	    		in = socket.getInputStream();
	        	br = new BufferedReader(new InputStreamReader(in, Constants.ENCODE_TYPE));

	        	sbRead = new StringBuffer();

	        	String telegram = null;

	        	while(true) {

	        		try {
	        			int c;    			

	        			if((c = br.read())!=-1) {

	        				sbRead.append((char) c);

	        				byte[] b = (sbRead.toString()).getBytes(Constants.ENCODE_TYPE);    		    					

	        				if(_DATA_LENGTH == b.length) {

	        					telegram = new String(b, Constants.ENCODE_TYPE);            	    					

	        					// 데이터부만 남기고 삭제
	        					telegram = telegram.substring(_DATA_LENGTH-_BODY_LENGTH);

	        					// 데이터을 파일 형태로 저장
	        					FileLogUtil.write(telegram);

	        					// 데이터처리	    		
	        					setTelegram(telegram);

	        					sbRead.setLength(0); // 버퍼초기화							

	        					telegram = null;
	        				}
	        			} else {					// client 가 정상적으로 연결 종료 한 경우
	        				log.info("Client has gone.");
	        				break;		  
	        			}
	        		}
	        		catch (IOException ex) {		// client 와 연결이 예상치 않게 끊어 지는 경우...
	    				log.debug(ex.getMessage());
	    				break;
	    			} catch(Exception ex) {
	    				log.debug("ERROR has occured"+ex.getMessage());
	    				break;
	    			}
	        	}
	        	
	        	if (br!=null) {
					br.close();
					log.info("The BufferedReader has closed");
				}
				if (in!=null) {
					in.close();
					log.info("The InputStream has closed");
				}
				if (socket!=null) {
					socket.close();
					log.info("The Socket has closed");
				}

				if ( state.equals("stop") ) {		// 소켓 스트림 read 루프 종료 했는데 커맨드 stop 이면 메인 스레드 종료.
					break;
				}
    		}
		} catch (IOException e) {
			log.info("An Unused ServerSocket has closed.");
			log.error("Server Error >> "+e.getMessage());
		} catch (Exception e) {
    		e.printStackTrace();
    		log.debug("RealTimeServer.run() Exception >>>>>>>>>>>>>>>>>>>>>>>> "+e.getMessage());
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
		    		log.error("RealTimeServer.run() Exception >>>>>>>>>>>>>>>>>>>>>>>> "+e.getMessage());
				}
		}

		log.info("");
		log.info("Realtime sales data receiving thread has terminated. ");
		//System.exit(0);
	}



	/**
	 * 자원 회수
	 */
	private void releaseSocketResources() {
		try {
			if (socket!=null && !socket.isClosed()) {
				socket.shutdownInput();
				socket.close();
				//log.info("소켓 해제 완료.");
			}
			
			if (serverSocket!=null ) {						// 서버 소켓 해제
				serverSocket.close();
				//log.info("서버소켓 해제 완료");
			}		

		} catch (IOException ex) {
			log.error("자원 회수 중 에러 발생 : {}",ex.getMessage());
		}
	}
	
	private void setTelegram(String telegram) throws Exception {
		
		byte[] bytes = telegram.getBytes(Constants.ENCODE_TYPE);
		
		String mbrNo = StringUtil.trimWhitespace(bytes, 3, 100);
		log.debug(String.format("THREAD_COUNT ---------------------------------------------------- %d", THREAD_COUNT));
		
		Runnable task = new SocketThread(telegram);
		
		do {
			String result = RealTimeServer.checkAndSubmitThread ( mbrNo, task );
			
			if (result.equals("COLLISION")) {			// mbrNo 충돌이면 collisionMap 에 보관하여 다음에 다시 처리 시도.
				synchronized(collisionMap) {
					collisionMap.put(attachUniqueKey(mbrNo), task);
				}
				log.info("###### mbrNo collision occured. This job will be kept in the collisionMap. map size : {}, mbrNo is {}", collisionMap.size(), mbrNo);
				break;
			} else if (result.equals("SUCCESS")) {		// 제대로 풀링 됐으면 종료
				break;
			} else { 									// 스레드 풀의 큐가 가득찬 경우 여기서 스레드 큐 빠질때 까지 재시도 2초 대기  (소켓 버퍼에는 쌓이겠지.. )
				log.info("######## The thread pool is full and waiting to be emptied. mbrNo : {}",mbrNo);
				Thread.sleep(2000);
			}
		} while(true);

	}




	/*******************************************************************
	/******************************************************************* 
	/*******************************************************************/
	
	public static void addToThreadCount() {
		synchronized (cntObj) {
			RealTimeServer.THREAD_COUNT++;
		}
	
	}
	
	public static void subtractFromThreadCount() {
		synchronized (cntObj) {
			if(RealTimeServer.THREAD_COUNT > 0) {
				RealTimeServer.THREAD_COUNT--;
			}
			
			if(RealTimeServer.THREAD_COUNT < 0) {
				RealTimeServer.THREAD_COUNT = 0;
			}			
		}
	}


	/**
	 * 입력받은 값을 유니크 하게 뒤에 년월일+난수를 덧붙인다.
	 * @param src
	 * @return
	 */
	private String attachUniqueKey(String src) {
		if (src == null )  {
			return null;
		}
		String random = String.format("__%s%s" , new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()), UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0,9));
		return src + random;
	}
	
	private String removeUniqueKey(String obj) {
		return obj.substring(0,obj.length()-25);
	}


	/**
	 * 스레드 풀의 큐 갯수 구하기.
	 * @return
	 */
	public static int getThreadPoolQueueSize() {
		int size = ((ThreadPoolExecutor)pool).getQueue().size();
		return size;
	}


	/**
	 * mbrNoList 에서 mbrNo 제거. 
	 * @param mbrNo
	 */
	public static void removembrNo(String mbrNo) {
		synchronized(mbrNoList) {
			mbrNoList.remove(mbrNo);
			log.info("###### mbrNo removed from the currently running mbrNo List. mbrNo List size : {}, mbrNo : [{}]", mbrNoList.size(), mbrNo);
		}
	}


	/**
	 * mbrNoList 에  실행하려는 task 의 mbrNo가 이미 있는지 확인하고 없으면 task 를 스레드 풀에.... 동기화 되어야 함.
	 * 
	 * @param mbrNo
	 * @param task
	 * @return 스레드 풀링 성공 여부. <br>SUCCESS: 스레드 풀링 성공 <br>COLLISION: 기존에 동일 mbrNo 매출건 처리중이라 대기 해야함 <br>FULL:스레드 풀의 큐가 가득참. 
	 */
	private static String checkAndSubmitThread(String mbrNo, Runnable task) {
		synchronized(mbrNoList) {
			if (mbrNoList.contains(mbrNo) == false ) {			// 기존에 동일 mbrNo 매출건이 처리 중이 아니면.
				try {
					pool.submit(task);						// 스레드 풀에 제출
					mbrNoList.add(mbrNo);								// 실행하려는 taks의 mbrNo를  mbrNoList에 add
					log.info("###### A new mbrNo is added to the thread pool. mbrNoList size : {}, mbrNo : [{}]", mbrNoList.size(), mbrNo);
					
					/*
				 	// 테스트
					for ( String cur_mbrNo : mbrNoList) {
						log.info("________ Currently Running mbrNo : [{}]", cur_mbrNo);
					}*/
					
				} catch ( RejectedExecutionException ex) {
					return "FULL";
				}
				return "SUCCESS";
			} else {
				return "COLLISION";
			}
		}
	}	


	/**
	 * safe shutdown 용 프로퍼티 파일 경로 리턴
	 * @param basename
	 * @return safe shutdown 용 프로퍼티 파일 경로
	 */
	public String getCommandPath(String basename) {
		
		String parentPath = null;
		
		if( _profile.equalsIgnoreCase("local") == false ) {
			File jarPath = new File(RealTimeServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
			parentPath = jarPath.getParentFile().getAbsolutePath();		// jar 파일로 실행되는 경우에는 jar 파일명 자체를 빼야 하기 때문에 부모 경로를 얻는다.
		} else {
			String jarPath = RealTimeServer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			parentPath = jarPath;						// 로컬일 때 (이클립스 실행)
		}
		
		String decodedPath = null;
		try {
			decodedPath = URLDecoder.decode(parentPath, "UTF-8");
		} catch (Exception ex ) {
			log.error(ex.getMessage());
		}
		decodedPath = decodedPath + basename;
		
		log.info("command properties : {}",  decodedPath+".properties");
		
		return decodedPath;
	}


	/**
	 * 스레드 풀 종료
	 */
	private void shutdownTreadPool() {
		pool.shutdown();
	}


	/**
	 * 서버 스레드 종료
	 */
	private void closeServerSocket() {
		try {
			if (serverSocket!=null && serverSocket.isClosed()==false) {						// 서버 소켓 해제
				serverSocket.close();
				log.info("The ServerSocket has closed");
			}		
		} catch (IOException ex) {
			log.error("서버 소켓 종료 중 에러 발생 : {} ", ex.getMessage() );
		}
	}




	/*******************************************************************
	/*******************************************************************
	/*******************************************************************
	
	
	/**
	 * safe shutdown 모니터 스래드
	 */
	public class CommandMonitor extends Thread {
		private final int interval = 1000;
		private String commandPath = null;
		private int shutdownStep = 0;			// 0 : 종료전, 1 : 종료시작 소켓 연결 해제, collisionMap 비워지길 기다림,  2: 스레드풀 셧 다운 시작 , 3: 완료. 

		@Override
		public void run() {

			commandPath = getCommandPath(_basename);
			
			messageSource =  new ReloadableResourceBundleMessageSource();
			messageSource.setBasename("file:"+commandPath);
			messageSource.setCacheSeconds(3);
			messageSource.setDefaultEncoding("UTF-8");
	    	
	    	
			Properties prop = new Properties();
			prop.setProperty("command", "run");		// 시작 시 run 으로 변경.
			
			try {
				log.info("resetting command properties : {} ", commandPath+".properties");
				prop.store(new FileOutputStream(commandPath+".properties"), "");
			} catch (Exception ex ) {
				log.error(ex.getMessage());
			}
	    	
			while(true) {
				try {
					state = messageSource.getMessage("command", null, Locale.US);
					//log.info("command : {} ",state);
					if (state.equals("stop")) {

						/*if ( isConnected ) {
							log.info("br.ready() :::: {} ",br.ready());
						}*/	
								
						if ( shutdownStep == 0 && ( sbRead.length() == 0)  ) {		// 소켓에서 읽을 데이터 없으면 
							shutdownStep = 1;
							log.info("");
							log.info("Shutdown step 1 >>>> Starting to close socket.. ");
							log.info("");
							releaseSocketResources();
						} else if ( shutdownStep == 1 && collisionMap.size() == 0 ) {			// 스텝 1인 상태에서 출동한 mbrNo보관용 맵이 빈 경우
							shutdownStep = 2;
							log.info("");
							log.info("Shutdown step 2 >>>> Starting to shutdown Thread Pool.. Please wait until all tasks have completed..");
							log.info("");
							shutdownTreadPool();
						} else if ( shutdownStep == 2 ) {
							shutdownStep = 3;
							log.info("");
							log.info("Shutdown step 3 >>>> Realtime sales data receiving server will be shutdown soon.");
							log.info("");
							//System.exit(0);				// 작업 스레드 종료 후 시스템 종료.
						} else if (shutdownStep == 3 ) {
							log.info("****************************************************************");
							log.info("****** Realtime sales data receiving server has shutdown. ******");
							log.info("****************************************************************");
							break;
						}
					}
					Thread.sleep(interval);
					
					/*log.debug("");
					log.debug(String.format("THREAD_COUNT ---------------------------------------------------- %d", THREAD_COUNT));
					log.debug(String.format("Thread Pool Queue size ------------------------------------------ %d", getThreadPoolQueueSize()));
					log.debug(String.format("Collision Map size --------------------------------------------------- %d", collisionMap.size()));*/
					
				} catch(InterruptedException ex) {
					log.error("Thread.sleep 에러 : {}",ex.getMessage());
				} /*catch (IOException e) {
					log.error("BufferedReader.ready 호출 중 에러 발생 : {} ",e.getMessage() );
				}*/
			}
			

		}
	}
	
	
	
	
	
	/**
	 * mbrNo 충돌난 매출건 재시도 스레드 
	 * 
	 * @author 
	 *
	 */
	public class Retryer implements Runnable {
		private final int interval = 2000;
		
		@Override
		public void run() {
			
			while(true) {

				synchronized(collisionMap) {
					if (state.equals("stop") && collisionMap.size() == 0 ) {		// shutdown 명령 & 실행 대기중 job 없으면 재시도 스레드 종료
						break;
					}
				
					if (collisionMap.size() > 0 ) {
	
						log.info("####### Start retry thread.... collisionMap size : {}", collisionMap.size());
						
						for(Iterator<Map.Entry<String, Runnable>> it = collisionMap.entrySet().iterator(); it.hasNext(); ) {
		
							Map.Entry<String, Runnable> entry = it.next();
							String mbrNo = removeUniqueKey(entry.getKey());
							log.info("####### Check if any Thread with the Same mbrNo is running.. mbrNo : [{}]", mbrNo);
							
							String result = RealTimeServer.checkAndSubmitThread( mbrNo, entry.getValue() );
							
							if (result.equals("SUCCESS")) {				// 풀링 성공이면 collisionMap 에서 스레드 제거..  아직 mbrNo 충돌이거나, 스레드 풀 큐가 가득 찼으면 대기. 
								it.remove();
								log.info("###### Finally, Long-awaited task has launched. remaining map size : {},  mbrNo : [{}]", collisionMap.size(), removeUniqueKey(entry.getKey()));
							}
						}
					}
				}
				
		        try {
		        	Thread.sleep(interval);
		        }	
		        catch(InterruptedException ex) {
					log.error("Thread.sleep 에러 : {}",ex.getMessage());
				}	        	
			}
		}
	}

}
