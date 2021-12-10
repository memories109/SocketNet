package com.realtime.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.realtime.dao.KakaoDao;
import com.realtime.message.KakaoMessage;

@Service
public class KakaoService {
	
	@Autowired
	private KakaoDao dao;
	
	public void saveData(KakaoMessage mesaage) {			
		dao.saveData(mesaage);
	}
}
