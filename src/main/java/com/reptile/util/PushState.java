package com.reptile.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.util.HashMap;
import java.util.Map;

public class PushState {
    private static Logger log = LoggerFactory.getLogger(PushState.class);


    public static void state(String UserCard, String approveName, int stat) {

//		application applications=new application();
        Map<String, Object> map1 = new HashMap<String, Object>();
        Map<String, Object> stati = new HashMap<String, Object>();
        Map<String, Object> data = new HashMap<String, Object>();
        stati.put("cardNumber", UserCard);
        stati.put("approveName", approveName);
        stati.put("approveState", stat + "");
        data.put("data", stati);
        Resttemplate resttemplatestati = new Resttemplate();
        log.warn(UserCard + "：本次推送内容 " + approveName + "   " + stat + "  推送地址为" + ConstantInterface.port);
        map1 = resttemplatestati.SendMessage(data, ConstantInterface.port + "/HSDC/authcode/Autherized");
        log.warn(UserCard + "：本次推送内容完成,推送结果为"+map1);
    }

    /**
     * @param UserCard
     * @param approveName
     * @param stat
     * @param message
     */
    public static void state(String UserCard, String approveName, int stat, String message) {
        if ("bankBillFlow".equals(approveName)) {
            message = "您提交的信用卡认证失败，失败原因：" + message + "，您可以重新认证或者选择其他产品。";
        } else if ("savings".equals(approveName)) {
            message = "您提交的储蓄卡认证失败，失败原因：" + message + "，您可以重新认证或者选择其他产品。";
        }
//		application applications=new application();
        Map<String, Object> map1 = new HashMap<String, Object>();
        Map<String, Object> stati = new HashMap<String, Object>();
        Map<String, Object> data = new HashMap<String, Object>();
        stati.put("cardNumber", UserCard);
        stati.put("approveName", approveName);
        stati.put("approveState", stat + "");
        stati.put("message", message);
        data.put("data", stati);
        log.warn(UserCard + "：本次推送内容 " + approveName + "   " + stat + "   "+message  +"  推送地址为" + ConstantInterface.port);
        Resttemplate resttemplatestati = new Resttemplate();
        map1 = resttemplatestati.SendMessage(data, ConstantInterface.port + "/HSDC/authcode/Autherized");
        log.warn(UserCard + "：本次推送内容完成,推送结果为"+map1);
    }

    /**
     * @param UserCard
     * @param approveName
     * @param stat
     * @param message
     */
    public static void stateX(String UserCard, String approveName, int stat, String message) {
        if ("bankBillFlow".equals(approveName)) {
            message = "您提交的信用卡认证失败，失败原因：" + message + "，您可以重新认证或者选择其他产品。";
        }
//		application applications=new application();
        Map<String, Object> map1 = new HashMap<String, Object>();
        Map<String, Object> stati = new HashMap<String, Object>();
        Map<String, Object> data = new HashMap<String, Object>();
        stati.put("cardNumber", UserCard);
        stati.put("approveName", approveName);
        stati.put("approveState", stat + "");
        stati.put("message", message);
        data.put("data", stati);

        log.warn(UserCard + "：本次推送内容 " + approveName + "   " + stat + "   "+message  +"  推送地址为" + ConstantInterface.port);
        Resttemplate resttemplatestati = new Resttemplate();
        map1 = resttemplatestati.SendMessage(data, ConstantInterface.port + "/HSDC/authcode/messagePush");
        log.warn(UserCard + "：本次推送内容完成,推送结果为"+map1);
    }
}
