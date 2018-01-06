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
	
	/**
	 * 
	 * @param UserCard
	 * @param approveName
	 * @param stat
	 * @param massage
	 */
	public static void state(String UserCard,String approveName ,int stat,String message){
		if("bankBillFlow".equals(approveName)){
	        message = "您提交的信用卡认证失败，失败原因："+message+"，您可以重新认证或者选择其他产品。";
	      }else if("savings".equals(approveName)){
	        message = "您提交的储蓄卡认证失败，失败原因："+message+"，您可以重新认证或者选择其他产品。";
	      }
//		application applications=new application();
		Map<String, Object> map1=new HashMap<String, Object>();
		Map<String,Object> stati=new HashMap<String, Object>();
		Map<String,Object> data=new HashMap<String, Object>();
		stati.put("cardNumber",UserCard);
		stati.put("approveName" , approveName);
		stati.put("approveState",stat+"");
		stati.put("message", message);
		data.put("data", stati);
		Resttemplate resttemplatestati = new Resttemplate();
	
	map1=resttemplatestati.SendMessage(data,ConstantInterface.port+"/HSDC/authcode/Autherized");
	}
	
	/**
	 * 
	 * @param UserCard
	 * @param approveName
	 * @param stat
	 * @param massage
	 */
	public static void stateX(String UserCard,String approveName ,int stat,String message){
		if("bankBillFlow".equals(approveName)){
	        message = "您提交的信用卡认证失败，失败原因："+message+"，您可以重新认证或者选择其他产品。";
	      }
//		application applications=new application();
		Map<String, Object> map1=new HashMap<String, Object>();
		Map<String,Object> stati=new HashMap<String, Object>();
		Map<String,Object> data=new HashMap<String, Object>();
		stati.put("cardNumber",UserCard);
		stati.put("approveName" , approveName);
		stati.put("approveState",stat+"");
		stati.put("message", message);
		data.put("data", stati);
		Resttemplate resttemplatestati = new Resttemplate();

	map1=resttemplatestati.SendMessage(data,ConstantInterface.port+"/HSDC/authcode/messagePush");
	}
}
