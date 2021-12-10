package com.realtime.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.realtime.App;
import com.realtime.service.SMSService;

@Component
public class SMSSender {

	
	private SMSService service;
	
	private String _from = "18996680";
	private String _to;
	
	private String _message;

	private static final Logger log = LoggerFactory.getLogger(SMSSender.class);
	
	public SMSSender() {
		
	}
	public String getFrom() {
		return _from;
	}
	public void setFrom(String from) {
		this._from = from;
	}
	public String getTo() {
		return _to;
	}
	public void setTo(String to) {
		this._to = to;
	}
	public SMSSender(String to) {
		_to = to;
	}
	public SMSSender(String from, String to) {
		_from = from;
		_to = to;
	}
	public void setMessage(String message) {
		_message = message;
	}	
	public String getMessage() {
		return _message;
	}
	
	public boolean send(String to) {
		
		if(_to == null || _to.length() == 0) {
			log.error("수신자가 없습니다.");
			return false;
		}
		else {
			
			if(_message == null || _message.length() == 0) {
				log.error("내용이 없습니다.");
				return false;
			}
			else {
				
				try {
				
					// 발송한다.
					// 서비스 할당
					service = App.context.getBean(SMSService.class);
					service.send(_from, _to, _message);
					
					return true;
					
				}
				catch(Exception e) {
					log.error("SMS 발송오류");
					e.printStackTrace();
					return false;
				}
			}
			
		}
	}
	
	public boolean send() {
		return send(_to);
	}
}

