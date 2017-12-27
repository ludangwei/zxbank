package com.reptile.util;

import java.util.HashMap;
import java.util.Map;

public class PushState {

	
	
	public static void state(String UserCard,String approveName ,int stat){
//		application applications=new application();
		Map<String, Object> map1=new HashMap<String, Object>();
		Map<String,Object> stati=new HashMap<String, Object>();
		Map<String,Object> data=new HashMap<String, Object>();
		stati.put("cardNumber",UserCard);
		stati.put("approveName" , approveName);
		stati.put("approveState",stat+"");
		data.put("data", stati);
		Resttemplate resttemplatestati = new Resttemplate();
	
	map1=resttemplatestati.SendMessage(data,ConstantInterface.port+"/HSDC/authcode/Autherized");
	}
}
