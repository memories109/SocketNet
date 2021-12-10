package com.realtime.dao;

import java.util.HashMap;

public interface SMSDao {

	public void send(HashMap<String, String> map);
	public void sendTest(HashMap<String, String> map);
	
}

