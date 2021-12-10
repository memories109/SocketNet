package com.realtime.service;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.realtime.dao.TestDao;
import com.realtime.model.TestModel;
import com.realtime.model.MemberInfoModel;

@Service
public class TestService {

	@Autowired
	private TestDao dao;
	
	public MemberInfoModel getMemberInfo(String mbrNo, String mbrNm) {
		
		HashMap<String, String> param = new HashMap<String, String>();
		
		param.put("mbrNo", mbrNo);
		param.put("mbrNm", mbrNm);
		
		return dao.getMemberInfo(param);
	}

	public void setData(TestModel testModel) {
		
		dao.setData(testModel);
		
	}

	public void setApplData(TestModel testModel) {
		dao.setApplData(testModel);
	}

}
