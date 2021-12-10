package com.realtime.dao;

import java.util.HashMap;

import com.realtime.model.TestModel;
import com.realtime.model.MemberInfoModel;

public interface TestDao {

	public MemberInfoModel getMemberInfo(HashMap<String, String> param);

	public void setData(TestModel testModel);

	public void setApplData(TestModel testModel);	

}
