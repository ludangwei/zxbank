package com.reptile.analysis;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONObject;
import org.json.XML;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.reptile.service.ZXBankService;
import com.reptile.util.ConstantInterface;
import com.reptile.util.CountTime;
import com.reptile.util.PushSocket;
import com.reptile.util.PushState;
import com.reptile.util.Resttemplate;

import net.sf.json.JSONArray;
@Service
public class ZXBanInfo {
	private Logger logger = LoggerFactory.getLogger(ZXBanInfo.class);
	public Map<String, Object> getDetailMes(HttpServletRequest request, String userCard, String phoneCode, String UUID,String timeCnt) throws ParseException {
    	boolean isok = false;
        Map<String, Object> map = new HashMap<String, Object>();
        HttpSession session = request.getSession();
        Object zxhttpClient = session.getAttribute("ZXhttpClient");

        isok = CountTime.getCountTime(timeCnt);
        Object zxImageCodeCook = session.getAttribute("zxCookies2");
        String flag="";
        PushSocket.pushnew(map, UUID, "1000","中信银行信用卡登陆中");
        if(isok==true) {
        	PushState.state(userCard, "bankBillFlow", 100);
        }
        if (zxhttpClient == null || zxImageCodeCook == null) {
            map.put("errorCode", "0001");
            map.put("errorInfo", "登录超时");
            PushSocket.pushnew(map, UUID, "3000","中信银行信用卡登陆失败");
            if(isok==true) {
            	PushState.state(userCard, "bankBillFlow", 200,"登录超时,登陆失败");
            }else {
            	PushState.stateX(userCard, "bankBillFlow", 200,"登录超时,登陆失败");
            }
            return map;
        } else {
            HttpClient httpClient = (HttpClient) zxhttpClient;
            String coks = zxImageCodeCook.toString();
            try {
                //提交短信验证码
                PostMethod postM = new PostMethod("https://creditcard.ecitic.com/citiccard/ucweb/checkSms.do?date=" + System.currentTimeMillis());
                postM.setRequestHeader("Cookie", coks);
                String str1 = "{smsCode:'" + phoneCode + "'}";
                RequestEntity entity1 = new StringRequestEntity(str1, "text/html", "utf-8");
                postM.setRequestEntity(entity1);
                httpClient.executeMethod(postM);
                postM.getParams().setContentCharset("utf-8");

                if (!postM.getResponseBodyAsString().contains("校验成功")) {
                    net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(postM.getResponseBodyAsString());
                    map.put("errorCode", "0001");
                    map.put("errorInfo", jsonObject.get("rtnMsg").toString());
                    PushSocket.pushnew(map, UUID, "3000","短信验证码提交出错,登陆失败");
                    if(isok==true) {
                    	PushState.state(userCard, "bankBillFlow", 200,"短信验证码提交出错,登陆失败");
                    }else {
                    	PushState.stateX(userCard, "bankBillFlow", 200,"短信验证码提交出错,登陆失败");
                    }
                    return map;
                }

                //成功进入信用卡信息页面
                GetMethod getMethod = new GetMethod("https://creditcard.ecitic.com/citiccard/newonline/myaccount.do?func=mainpage");
                getMethod.setRequestHeader("Cookie", coks);
                httpClient.executeMethod(getMethod);
                getMethod.getParams().setContentCharset("utf-8");
                
                if (getMethod.getResponseBodyAsString().contains("您还未绑卡，暂不支持业务办理")) {
                    map.put("errorCode", "0003");
                    map.put("errorInfo", "您还未绑卡，暂不支持业务办理");
                    PushSocket.pushnew(map, UUID, "3000","中信银行信用卡登陆失败，您还未绑卡，暂不支持业务办理");
                    if(isok==true) {
                    	PushState.state(userCard, "bankBillFlow", 200,"中信银行信用卡登陆失败，您还未绑卡，暂不支持业务办理");
                    }else {
                    	PushState.stateX(userCard, "bankBillFlow", 200,"中信银行信用卡登陆失败，您还未绑卡，暂不支持业务办理");
                    }
                    return map;
                }
                PushSocket.pushnew(map, UUID, "2000","中信银行登陆成功");
                Thread.sleep(1000);
                PushSocket.pushnew(map, UUID, "5000","中信银行信息获取中");
                flag="5000";
                //查询信用卡额度及可提现额度
                PostMethod method = new PostMethod("https://creditcard.ecitic.com/citiccard/newonline/settingManage.do?func=getCreditLimit");
                method.setRequestHeader("Cookie", coks);
                httpClient.executeMethod(method);
                String result = method.getResponseBodyAsString();
                JSONObject jsonObject3 = XML.toJSONObject(result);
                JSONObject response2 = (JSONObject) jsonObject3.get("response");
                JSONObject creditLimit = (JSONObject) response2.get("CreditLimit");
                String cashmoney = creditLimit.get("cashmoney").toString();
                String fixedEd = creditLimit.get("fixedEd").toString();

                //查询该账号下对应的银行卡信息有几个
                PostMethod postMethod = new PostMethod("https://creditcard.ecitic.com/citiccard/newonline/common.do?func=querySignCards");
                postMethod.setRequestHeader("Cookie", coks);
                httpClient.executeMethod(postMethod);

                //将得到的xml转换为json数据
                String cardResult = postMethod.getResponseBodyAsString();
                JSONObject jsonObject = XML.toJSONObject(cardResult);
                String response1 = jsonObject.get("response").toString();
                net.sf.json.JSONObject jsonObject1 = net.sf.json.JSONObject.fromObject(response1);
                net.sf.json.JSONArray cardlist1=new JSONArray();
                try{
                    cardlist1 = jsonObject1.getJSONArray("cardlist");
                }catch (Exception e){
                    Object cardlist = jsonObject1.get("cardlist");
                    cardlist1.add(cardlist);
                    logger.warn("中信银行获取卡列表失败 mrldw",e);
                    PushSocket.pushnew(map, UUID, "7000","中信银行信息获取失败");
                    if(isok==true) {
                    	PushState.state(userCard, "bankBillFlow", 200,"中信银行获取卡列表失败");
                    }else {
                    	PushState.stateX(userCard, "bankBillFlow", 200,"中信银行获取卡列表失败");
                    }
                }


                List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
                int countError=0;
                for (int i = 0; i < cardlist1.size(); i++) {
                    net.sf.json.JSONObject jsonObject2 = cardlist1.getJSONObject(i);
                    String card_nbr = jsonObject2.get("card_nbr").toString();
                    String card_des = jsonObject2.get("card_desc").toString();
                    if (!card_des.contains("银联")) {
                        continue;
                    }
                    PostMethod postMethod1 = new PostMethod("https://creditcard.ecitic.com/citiccard/newonline/billQuery.do?func=queryBillInfo");
                    postMethod1.setRequestHeader("Cookie", coks);
                    NameValuePair param1 = new NameValuePair("cardNo", card_nbr);
                    NameValuePair param2 = new NameValuePair("stmt_date", "");
                    NameValuePair param3 = new NameValuePair("crytype", "156");
                    NameValuePair param4 = new NameValuePair("start_pos", "1");
                    NameValuePair param5 = new NameValuePair("count", "12");
                    NameValuePair param6 = new NameValuePair("rowsPage", "10");
                    NameValuePair param7 = new NameValuePair("startpos", "1");
                    postMethod1.setRequestBody(new NameValuePair[]{param1, param2, param3, param4, param5, param6, param7});
                    httpClient.executeMethod(postMethod1);

                    JSONObject json = XML.toJSONObject(postMethod1.getResponseBodyAsString());
                    String response = json.get("response").toString();
                    net.sf.json.JSONObject jsonO = net.sf.json.JSONObject.fromObject(response);
                    net.sf.json.JSONArray cardlist = jsonO.getJSONArray("billsMonthList");

                    int size = 0;
                    if (cardlist != null) {
                        if (cardlist.size() > 6) {
                            size = 6;
                        } else {
                            size = cardlist.size();
                        }
                    }

                    for (int j = 0; j < size; j++) {
                        PostMethod postMeth = new PostMethod("https://creditcard.ecitic.com/citiccard/newonline/billQuery.do?func=queryBillInfo");
                        postMeth.setRequestHeader("Cookie", coks);
                        NameValuePair param11 = new NameValuePair("cardNo", card_nbr);
                        NameValuePair param21 = new NameValuePair("stmt_date", cardlist.getJSONObject(j).get("stmt_date").toString());
                        NameValuePair param31 = new NameValuePair("crytype", "156");
                        NameValuePair param41 = new NameValuePair("start_pos", "1");
                        NameValuePair param51 = new NameValuePair("count", "12");
                        NameValuePair param61 = new NameValuePair("rowsPage", "10");
                        NameValuePair param71 = new NameValuePair("startpos", "1");
                        postMeth.setRequestBody(new NameValuePair[]{param11, param21, param31, param41, param51, param61, param71});
                        httpClient.executeMethod(postMeth);

                        String dataResult=postMeth.getResponseBodyAsString();
                        logger.warn("------------中信银行信用卡获取账单:---------------"+dataResult);
                        if(!dataResult.contains("网络繁忙")){
                        	Map<String, Object> infoData = new HashMap<String, Object>();
                        	infoData = getInfos(dataResult,fixedEd);
                            dataList.add(infoData);
                        }else{
                            countError++;
                        }
                        Thread.sleep(500);
                    }
                }

                if(countError>1){
                    logger.warn(userCard+":中信银行信用卡获取账单过程中出现账单内容为网络繁忙，false--------errorCount次数为"+countError);
                    PushSocket.pushnew(map, UUID, "7000", "数据获取过程中出现网络故障");
                    PushState.state(userCard, "bankBillFlow", 200, "数据获取过程中出现网络故障");
                    map.put("errorCode", "0009");
                    map.put("errorInfo", "数据获取不完全，请重新再次认证！");
                    return map;
                }
                logger.warn(userCard+":中信银行信用卡获取账单过程中出现账单为网络繁忙，正常--------errorCount次数为"+countError);

                PushSocket.pushnew(map, UUID, "6000","中信银行信息获取成功");
                flag="6000";

                Map<String, Object> sendMap = new HashMap<String, Object>();
	            sendMap.put("bankList", dataList);

	            
                logger.warn(sendMap.toString()+"   mrlu");
                //推送信息
                Map<String, Object> mapTui = new HashMap<String, Object>();
                mapTui.put("data", sendMap);
                mapTui.put("backtype", "CCB");
	            mapTui.put("idcard", userCard);
	            mapTui.put("bankname", "中信");
	            mapTui.put("userAccount",phoneCode);	            
	            
	            
	            
	            
                Resttemplate rs = new Resttemplate();
                map = rs.SendMessage(mapTui, ConstantInterface.port + "/HSDC/BillFlow/BillFlowByreditCard");
                if(map!=null&&"0000".equals(map.get("errorCode").toString())){
                    map.put("errorInfo","查询成功");
                    map.put("errorCode","0000");
                    PushSocket.pushnew(map, UUID, "8000","中信银行信用卡认证成功");
                    if(isok==true) {
                     	PushState.state(userCard, "bankBillFlow", 300);
                     }
                }else{
                	//--------------------数据中心推送状态----------------------
                	 PushSocket.pushnew(map, UUID, "9000","中信银行信用卡认证失败"+map.get("errorInfo").toString());
                	 if(isok==true) {
                     	PushState.state(userCard, "bankBillFlow", 200,map.get("errorInfo").toString());
                     }else {
                     	PushState.stateX(userCard, "bankBillFlow", 200,map.get("errorInfo").toString());
                     }
                	//---------------------数据中心推送状态----------------------
//                	logger.warn("光大银行储蓄卡推送失败"+IDNumber);
                	
                }
            } catch (Exception e) {
            	if(flag.equals("5000")){
					PushSocket.pushnew(map, UUID, "7000","中信信用卡账单获取失败");
				}else if(flag.equals("6000")){
					PushSocket.pushnew(map, UUID, "9000","中信信用卡，认证失败");
				}else{
					PushSocket.pushnew(map, UUID, "3000","中信信用卡，登录失败");
				}
                logger.warn(e.getMessage() + "  中信获取账单   mrlu",e);
                if(isok==true) {
                 	PushState.state(userCard, "bankBillFlow", 200,"网络异常，数据获取失败");
                 }else {
                 	PushState.stateX(userCard, "bankBillFlow", 200,"网络异常，数据获取失败");
                 }
                map.put("errorCode", "0002");
                map.put("errorInfo", "查询出错");
            }        
        }
	
        return map;
    }

	
	public static Map<String, Object> getInfos(String dataResult,String fixedEd) {
		Map<String, Object> bankList = new HashMap<String, Object>();
		Map<String, Object> accountSummary = new HashMap<String, Object>();
		Document doc = Jsoup.parse(dataResult);
		Element billProfile = doc.getElementsByTag("billProfile").get(0);
		String RMBCurrentAmountDue = billProfile.attr("bq_paied").replace(",", "");//本期应还款额
		String RMBMinimumAmountDue = billProfile.attr("min_pay").replace(",", "");//最低还款
		String PaymentDueDate = billProfile.attr("dte_pymt_due").replace("年", "").replace("月", "").replace("日", "");//到期还款日
		String StatementDate = billProfile.attr("stmt_date_text").replace("年", "").replace("月", "").replace("日", "");//本期账单日
		accountSummary.put("RMBCurrentAmountDue", RMBCurrentAmountDue);
		accountSummary.put("RMBMinimumAmountDue", RMBMinimumAmountDue);
		accountSummary.put("PaymentDueDate", PaymentDueDate);
		accountSummary.put("StatementDate", StatementDate);
		accountSummary.put("CreditLimit", fixedEd);
		
		Elements billDetailList = doc.getElementsByTag("billDetailList");
		List<Object> payRecordList = new ArrayList<Object>();
		for (Element billDetail : billDetailList) {
			Map<String, Object> payRecord = new HashMap<String, Object>();
			payRecord.put("tran_date", billDetail.attr("post_date").replace("-", ""));//日期
			payRecord.put("tran_desc", billDetail.attr("tran_desc").trim());//简介描述
			payRecord.put("post_amt", billDetail.attr("post_amt").replace(",", ""));//金额
			payRecordList.add(payRecord);//每月账单明细
		}
		bankList.put("payRecord", payRecordList);
		bankList.put("AccountSummary", accountSummary);
		return bankList;		
	}
	public static void main(String[] args) {
		Map<String,Object> map=net.sf.json.JSONObject.fromObject("{\"userAccount\":\"6226580046023293\",\"idcard\":\"610111199203252021\",\"data\":{\"bankList\":[{\"AccountSummary\":{\"RMBMinimumAmountDue\":\"1000.00\",\"CreditLimit\":\"20000.00\",\"RMBCurrentAmountDue\":\"20000.00\",\"StatementDate\":\"20180212\",\"PaymentDueDate\":\"20180303\"},\"payRecord\":[{\"tran_date\":\"20180131\",\"tran_desc\":\"已收妥您的款项\",\"post_amt\":\"15375.00\"},{\"tran_date\":\"20180201\",\"tran_desc\":\"（特约）北京至尊荟 广东省\",\"post_amt\":\"1000.00\"},{\"tran_date\":\"20180201\",\"tran_desc\":\"陕西省西安市雁塔区 西安市\",\"post_amt\":\"14000.00\"},{\"tran_date\":\"20180205\",\"tran_desc\":\"（特约）北京至尊荟 广东省\",\"post_amt\":\"5000.00\"}]},{\"AccountSummary\":{\"RMBMinimumAmountDue\":\"1125.00\",\"CreditLimit\":\"20000.00\",\"RMBCurrentAmountDue\":\"15375.00\",\"StatementDate\":\"20180112\",\"PaymentDueDate\":\"20180131\"},\"payRecord\":[{\"tran_date\":\"20171221\",\"tran_desc\":\"西安市新城区宏轩服\",\"post_amt\":\"15000.00\"},{\"tran_date\":\"20171221\",\"tran_desc\":\"已收妥您的款项\",\"post_amt\":\"398.85\"},{\"tran_date\":\"20180112\",\"tran_desc\":\"2月份账单取现分期12期：本期应还款375.00，余额0.00，余期为00期\",\"post_amt\":\"0.00\"}]},{\"AccountSummary\":{\"RMBMinimumAmountDue\":\"398.85\",\"CreditLimit\":\"20000.00\",\"RMBCurrentAmountDue\":\"398.85\",\"StatementDate\":\"20171212\",\"PaymentDueDate\":\"20171231\"},\"payRecord\":[{\"tran_date\":\"20171204\",\"tran_desc\":\"违约金\",\"post_amt\":\"18.75\"},{\"tran_date\":\"20171209\",\"tran_desc\":\"已收妥您的款项\",\"post_amt\":\"375.00\"},{\"tran_date\":\"20171212\",\"tran_desc\":\"分期利息\",\"post_amt\":\"5.10\"},{\"tran_date\":\"20171212\",\"tran_desc\":\"2月份账单取现分期12期：本期应还款375.00，余额375.00 ，余期为01期\",\"post_amt\":\"0.00\"}]},{\"AccountSummary\":{\"RMBMinimumAmountDue\":\"375.00\",\"CreditLimit\":\"20000.00\",\"RMBCurrentAmountDue\":\"375.00\",\"StatementDate\":\"20171112\",\"PaymentDueDate\":\"20171201\"},\"payRecord\":[{\"tran_date\":\"20171030\",\"tran_desc\":\"已收妥您的款项\",\"post_amt\":\"400.15\"},{\"tran_date\":\"20171112\",\"tran_desc\":\"2月份账单取现分期12期：本期应还款375.00，余额750.00 ，余期为02期\",\"post_amt\":\"0.00\"}]},{\"AccountSummary\":{\"RMBMinimumAmountDue\":\"400.15\",\"CreditLimit\":\"20000.00\",\"RMBCurrentAmountDue\":\"400.15\",\"StatementDate\":\"20171012\",\"PaymentDueDate\":\"20171031\"},\"payRecord\":[{\"tran_date\":\"20171004\",\"tran_desc\":\"违约金\",\"post_amt\":\"19.93\"},{\"tran_date\":\"20171008\",\"tran_desc\":\"上海富友支付服务有限 上海市\",\"post_amt\":\"398.50\"},{\"tran_date\":\"20171012\",\"tran_desc\":\"分期利息\",\"post_amt\":\"5.22\"},{\"tran_date\":\"20171012\",\"tran_desc\":\"2月份账单取现分期12期：本期应还款375.00，余额1,125.00 ，余期为03期\",\"post_amt\":\"0.00\"}]},{\"AccountSummary\":{\"RMBMinimumAmountDue\":\"398.50\",\"CreditLimit\":\"20000.00\",\"RMBCurrentAmountDue\":\"398.50\",\"StatementDate\":\"20170912\",\"PaymentDueDate\":\"20171001\"},\"payRecord\":[{\"tran_date\":\"20170903\",\"tran_desc\":\"违约金\",\"post_amt\":\"18.75\"},{\"tran_date\":\"20170906\",\"tran_desc\":\"已收妥您的款项\",\"post_amt\":\"375.00\"},{\"tran_date\":\"20170912\",\"tran_desc\":\"分期利息\",\"post_amt\":\"4.75\"},{\"tran_date\":\"20170912\",\"tran_desc\":\"2月份账单取现分期12期：本期应还款375.00，余额1,500.00 ，余期为04期\",\"post_amt\":\"0.00\"}]}]},\"backtype\":\"CEB\",\"bankname\":\"光大银行信用卡\"}");
		Resttemplate rs = new Resttemplate();
	    map = rs.SendMessage(map, ConstantInterface.port + "/HSDC/BillFlow/BillFlowByreditCard");
	    System.out.println(map.toString());
	}

}
















