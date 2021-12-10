package com.realtime.message;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.realtime.App;
import com.realtime.service.KakaoService;

public class KakaoMessage {
	
	private static final Logger log = LoggerFactory.getLogger(KakaoMessage.class);
	
	protected static final String DOMAIN 	 = 	"https://alimtalk-api.sweettracker.net";
	protected static final String PROFILE_KEY = "96005b0924499cebf8837045977e8c8c0de8a989";
	
	// 메시지를 전송합니다.
	public void send() throws Exception {
		
		// 메시지 확정
		log.debug(message);
		
		// 전송준비
		BufferedReader in = null;			
		OutputStream os = null;
		
		try {
			
			// 전송 URL
			String callUrl = String.format("%s/v1/%s/sendMessage/"
					, DOMAIN
					, PROFILE_KEY);
			
			// 발송을 위한 json 문자열을 만듭니다.
			String requestData = getRequestJsonString();
			
			log.debug("+==========================================================+");
			log.debug("| 카카오알림톡 REQUEST >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> |");
			log.debug("| callUrl : " + callUrl);
			log.debug("| requestData : "+ requestData);
			log.debug("+==========================================================+");			
			
			byte[] postDataBytes = requestData.getBytes("UTF-8");
			   
			HttpURLConnection connection = (HttpURLConnection)new URL(callUrl).openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "Application/json");
			connection.setDoOutput(true);
	        os = connection.getOutputStream();
	        os.write(postDataBytes);
	        os.flush();
	        
	        //---------------- 응답 --------------------------------------------------------------
			
	        if (200 <= connection.getResponseCode() && connection.getResponseCode() <= 299) {
	        	in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			} else {
				in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
			}
	        
	        String line = null;
		    String jsonResponse = "";
		    
		    while((line = in.readLine())!=null)
		    {
		    	jsonResponse += line;
		    }
		    
		    //응답값
		    try {
				JSONArray array = new JSONArray(jsonResponse);
		    	for(int i=0; i< array.length(); i++){
					JSONObject jo = array.getJSONObject(i);
					result = (jo.has("result") ? jo.get("result").toString() : "ERROR" );
					resultCode = (jo.has("code") ? jo.get("code").toString() : "ERROR" );
					resultMessage = (jo.has("error") ? jo.get("error").toString() : "" );
				}
			}
			catch(Exception ex) {
				throw ex;
			}
		    
		    log.debug("+==========================================================+");
			log.debug("| 카카오알림톡 RESPONSE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> |");
			log.debug("| callUrl : " + callUrl);
			log.debug("| jsonResponse : " + jsonResponse);
			log.debug("| result : " + result);
			log.debug("| code : " + resultCode);
			log.debug("| message : " + resultMessage);
			log.debug("+==========================================================+");
			
			// DB에 히스토리 저장
			KakaoService service = App.context.getBean(KakaoService.class);
			service.saveData(this);
			
			/*
			[ERROR] 2018-08-07 02:20:19,284: com.altime.message.KakaoMesage#send: +==========================================================+
			[ERROR] 2018-08-07 02:20:19,285: com.altime.message.KakaoMesage#send: | 카카오알림톡 REQUEST >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> |
			[ERROR] 2018-08-07 02:20:19,286: com.altime.message.KakaoMesage#send: | callUrl : https://alimtalk-api.sweettracker.net/v1/96005b0924499cebf8837045977e8c8c0de8a989/sendMessage/
			[ERROR] 2018-08-07 02:20:19,288: com.altime.message.KakaoMesage#send: | requestData : [{"reserved_time":"00000000000000","receiver_num":"01034449570","sms_kind":"S","profile_key":"96005b0924499cebf8837045977e8c8c0de8a989","sender_num":"18990522","sms_message":"[포인트차감] 아시아나항공(주) 152,460P / 잔여 : 0P","msgid":"4986181","sms_title":"","message":"주간연속2교대몰\n[포인트 차감]\n\n▶ 차감 포인트 : 152,460P\n▶ 차감 전 가용 포인트 : 152,460P\n▶ 차감 후 잔여 포인트 : 0P\n\n(카드)사용일시 : 2018/08/07 02:09 \n(카드)사용처 : 아시아나항공(주)\n(카드)사용금액 : 667,400원 \n\n▣안내사항 \n- 본 차감건은 주간연속2교대 포인트를 통하여 차감되는 건으로 실제 \n카드결제 건에서는 제외 또는 환급될 수 있습니다.\n\n▣[고객센터] 1899-6680\n-상담시간 안내 \n평일 09:00~18:00 \n점심 12:00~13:00 \n토, 일, 공휴일 휴무","button1":{"url_mobile":"https://cert.benecafe.co.kr/member/login?&cmpyNo=Q15&lc=https://cert.benecafe.co.kr/mywel/costApplicationListCo?dispCatNo=","name":"자세히 보기 / 차감취소","type":"WL"},"template_code":"realtime_deduct_h_v2"}]
			[ERROR] 2018-08-07 02:20:19,289: com.altime.message.KakaoMesage#send: +==========================================================+
			[ERROR] 2018-08-07 02:20:19,554: com.altime.message.KakaoMesage#send: +==========================================================+
			[ERROR] 2018-08-07 02:20:19,556: com.altime.message.KakaoMesage#send: | 카카오알림톡 RESPONSE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> |
			[ERROR] 2018-08-07 02:20:19,557: com.altime.message.KakaoMesage#send: | callUrl : https://alimtalk-api.sweettracker.net/v1/96005b0924499cebf8837045977e8c8c0de8a989/sendMessage/
			[ERROR] 2018-08-07 02:20:19,559: com.altime.message.KakaoMesage#send: | jsonResponse : [{"result":"Y","code":"K000","kind":"K","msgid":"4986181","error":"","sendtime":"2018-08-07 02:20:20"}]
			[ERROR] 2018-08-07 02:20:19,560: com.altime.message.KakaoMesage#send: | result : Y
			[ERROR] 2018-08-07 02:20:19,561: com.altime.message.KakaoMesage#send: | code : K000
			[ERROR] 2018-08-07 02:20:19,563: com.altime.message.KakaoMesage#send: | message : 
			[ERROR] 2018-08-07 02:20:19,564: com.altime.message.KakaoMesage#send: +==========================================================+
			*/
		}
		catch(Exception ex) {
		
			if ( in != null ) try {in.close();}catch(Exception e){}
			if ( os != null ) try {os.close();}catch(Exception e){}
			
			throw ex;
		}
	}
	
	// 응답문자열 구성
	private String getRequestJsonString() {
		
		JSONObject requestData = new JSONObject();
		JSONArray requestDataList = new JSONArray();
		
		msgid = String.format("_%s%s"
				, new SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
				, UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0,9)
			).substring(0,20); // 1+14+5 = 20
		
		//요청 파라미터 
		requestData.put("msgid", msgid);
		requestData.put("profile_key", PROFILE_KEY);
		requestData.put("template_code", templateCode);
		requestData.put("receiver_num", receiverNumber);
		requestData.put("message", message); 
		requestData.put("reserved_time", reservedTime);
		requestData.put("sms_message", smsMessage);  			//카카오 비즈메시지 발송이 실패했을 때 SMS전환발송을 위한 메시지
		requestData.put("sms_title", "");						//LMS발송을 위한 제목
		requestData.put("sms_kind", smsKind); 					//전환발송 시 SMS/LMS 구분(SMS : S, LMS : L, 발송 안함 : N) SMS 대체발송을 사용하지 않는 경우 : N
		requestData.put("sender_num", senderNumber);			// SMS발신번호		
		
		// 버튼추가
		for(Map.Entry<String, JSONObject> entry : buttons.entrySet()) {
			
			String name = entry.getKey();
			JSONObject button = entry.getValue();	
			
			requestData.put(name, button);
		}
		
		// 요청문자열을 요청열에 추가
		requestDataList.put(requestData);
		
		return requestDataList.toString();
	}
	
	protected HashMap<String, JSONObject> buttons = new  HashMap<String, JSONObject>();
	
	public String getProfileKey() {
		return PROFILE_KEY;
	} 
	
	protected String senderNumber = "18990522"; // 발신번호
	
	public String getSenderNumber() {
		return senderNumber;
	}

	protected String templateCode = null;
	
	public String getTemplateCode() {
		return templateCode;
	}

	protected String message = null;
	
	public String getMessage() {
		return message;
	}	

	protected String smsMessage = null;
	
	public String getSmsMessage() {
		return smsMessage;
	}
	
	private String smsKind = "S";
	
	
	public String getSmsKind() {
		return smsKind;
	}

	public void setSmsKind(String smsKind) {
		this.smsKind = smsKind;
	}	
	
	protected String receiverNumber;

	public String getReceiverNumber() {
		return receiverNumber;
	}

	public void setReceiverNumber(String receiverNumber) {
		this.receiverNumber = receiverNumber.replaceAll("-", "");
	}
	
	private String reservedTime = "00000000000000"; // 즉시발송만사용, 예약전송 :20160310210000
	
	public String getReservedTime() {
		return reservedTime;
	}
	
	private String msgid;
	
	public String getMsgid() {
		return msgid;
	}

	public void setMsgid(String msgid) {
		this.msgid = msgid;
	}

	// 응답
	private String result;
	private String resultCode;
	private String resultMessage;
	
	public String getResult() {
		return result;
	}

	public String getResultCode() {
		return resultCode;
	}	

	public String getResultMessage() {
		return resultMessage;
	}
}
