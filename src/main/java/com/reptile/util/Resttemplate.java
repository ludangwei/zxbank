package com.reptile.util;

import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * @author bigyoung
 * @version 1.0
 * 
 *
 */
public class Resttemplate {
	Logger logger= LoggerFactory.getLogger(Resttemplate.class);
	/**
	 * 
	 * @param map 需要推送的数据
	 * @param Url 推送的地址
	 * @return 返回推送状态
	 */
//  public Map<String,Object> SendMessage(Map<String,Object> map,String Url){
//	  Map<String,Object> message=new HashMap<String, Object>();
//	  try {
//		  StringHttpMessageConverter m = new StringHttpMessageConverter(Charset.forName("UTF-8"));
//		  RestTemplate restTemplate = new RestTemplateBuilder().additionalMessageConverters(m).build();
//          HttpHeaders headers = new HttpHeaders();
//          headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//          MediaType type = MediaType.parseMediaType("application/x-www-form-urlencoded; charset=UTF-8");
//          headers.setContentType(type);
//          headers.add("Accept", MediaType.APPLICATION_JSON.toString());
//          logger.warn("推送信息为："+JSONObject.fromObject(map).toString());
//          System.out.println(JSONObject.fromObject(map).toString()+"sssvvvv");
//          HttpEntity<String> formEntity = new HttpEntity<String>(JSONObject.fromObject(map).toString(), headers);
//          String result = restTemplate.postForObject(Url, formEntity,String.class);
//          JSONObject jsonObject= JSONObject.fromObject(result);
//          if(jsonObject.get("errorCode").equals("0000")){
//        		message.put("errorCode","0000");
//    			message.put("errorInfo","查询成功");
//          }else{
//        		message.put("errorCode",jsonObject.get("errorCode"));//异常处理
//    			message.put("errorInfo",jsonObject.get("errorInfo"));
//          }
//           
//		} catch (Exception e) {
//	  		logger.warn("推送数据过程中出现错误 loggerError",e);
//	  		e.printStackTrace();
//			message.put("errorCode","0003");//异常处理
//			message.put("errorInfo","推送失败");
//		}
//	  	return message;
//	  
//  }
  
  
  /**
	 * 数据中心新推口推送方法
	 * @param map 需要推送的数据
	 * @param Url 推送的地址
	 * @return 返回推送状态
	 */
  public Map<String,Object> SendMessage(Map<String,Object> map,String Url){
	  logger.warn("入参信息为："+JSONObject.fromObject(map).toString());
	  Map<String,Object> message=new HashMap<String, Object>();
	  try {
		  
		  	String par="";
		  	StringHttpMessageConverter m = new StringHttpMessageConverter(Charset.forName("UTF-8"));
		  	RestTemplate restTemplate = new RestTemplateBuilder().additionalMessageConverters(m).build();
		  	HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
	        MediaType type = MediaType.parseMediaType("application/x-www-form-urlencoded; charset=UTF-8");
	        headers.setContentType(type);
	        headers.add("Accept", MediaType.APPLICATION_JSON.toString());	        
	        Map<String,String> datamap = new HashMap<String,String>();
	        datamap.put("data", JSONObject.fromObject(map).toString());
	        
	        if(datamap!=null){
		        Iterator<String> iter = datamap.keySet().iterator(); 
		          while(iter.hasNext()){ 
		              String key=iter.next(); 
		              Object value = datamap.get(key);
		             par=par+key+"="+value+"&";
		          }
		          par=par.substring(0,par.length()-1);
		    }	        
	        logger.warn("推送信息为："+par);
	        HttpEntity<String> formEntity = new HttpEntity<String>(par, headers);
	        String result = restTemplate.postForObject(Url, formEntity,String.class);
	        JSONObject jsonObject= JSONObject.fromObject(result);
	        if(jsonObject.get("errorCode").equals("0000")){
	      		message.put("errorCode","0000");
	  			message.put("errorInfo","查询成功");
	        }else{
	      		message.put("errorCode",jsonObject.get("errorCode"));//异常处理
	  			message.put("errorInfo",jsonObject.get("errorInfo"));
	        }
	        logger.warn("****推送完成****推送结果*********"+jsonObject);
		} catch (Exception e) {
	  		logger.warn("推送数据过程中出现错误 loggerError",e);
	  		e.printStackTrace();
			message.put("errorCode","0003");//异常处理
			message.put("errorInfo","推送失败");
		}
	  	return message;
	  
	}
  
  

}
