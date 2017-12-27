package com.reptile.util;

import net.sf.json.JSONObject;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ldw on 2017-9-14
 * <p>
 * 使用注解形式的aop，在类明上增加@aspect注解
 * 通过aop的方式返回认证状态
 */
@Component
@Aspect
public class AopClass {
    private Logger logger = LoggerFactory.getLogger(AopClass.class);

    @Pointcut("@annotation(com.reptile.util.CustomAnnotation)")
    public void pointCut() {
    }

    @Around("pointCut()")
    public  Map<String,Object> around(ProceedingJoinPoint joinPoint) throws Throwable {
        Map<String,Object> map=new HashMap<String,Object>();
        try {
            String className = joinPoint.getSignature().getDeclaringTypeName(); //所调用类名全称
            String argsName = joinPoint.getArgs()[1].toString();//所调方法第一个参数
            logger.warn("正在推送认证中状态");
            Map<String, Object> stringObjectMap = beforeTuiSong(className, argsName);
            logger.warn("推送状态完毕");
            if(stringObjectMap.toString().contains("2222")){
                map.put("errorCode","0001");
                map.put("errorInfo","该账号未实名认证！");
                return map;
            }else{
                map= (Map<String, Object>) joinPoint.proceed();
                className = joinPoint.getSignature().getDeclaringTypeName(); //所调用类名全称
                argsName = joinPoint.getArgs()[1].toString();//所调方法第一个参数
                String approveState = "200";
                JSONObject jsonObject = JSONObject.fromObject(map);
                if (jsonObject.get("errorCode").equals("0000")) {
                    approveState = "300";
                }
                logger.warn("正在推送认证结果");
                afterTuiSong(className, argsName, approveState);
                logger.warn("推送认证结果完成");
            }
        } catch (Exception e) {
            logger.warn("mrlu认证状态推送失败" + e.getMessage(),e);
            e.printStackTrace();
            map.put("errorCode","0001");
            map.put("errorInfo","推送状态失败");
        }
        return map;
    }

    //认证前推送状态
    public Map<String, Object> beforeTuiSong(String className, String argsName) {
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> dataMap = new HashMap<String, String>();

        if (className.contains("BankController")) {
            dataMap.put("approveName", "bankBillFlow");
        }
        if (className.contains("DepositCardController")) {
            dataMap.put("approveName", "savings");
        }

        dataMap.put("cardNumber", argsName);
        dataMap.put("approveState", "100");
        map.put("data", dataMap);
        Resttemplate resttemplate = new Resttemplate();

        Map<String, Object> mapResult = resttemplate.SendMessage(map, ConstantInterface.port +"/HSDC/authcode/Autherized");

        return mapResult;
    }


    //认证后推送状态
    public Map<String, Object> afterTuiSong(String className, String argsName, String approveState) {
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> dataMap = new HashMap<String, String>();

        if (className.contains("BankController")) {
            dataMap.put("approveName", "bankBillFlow");
        }
        if (className.contains("DepositCardController")) {
            dataMap.put("approveName", "savings");
        }
        dataMap.put("cardNumber", argsName);
        dataMap.put("approveState", approveState);
        map.put("data", dataMap);
        Resttemplate resttemplate = new Resttemplate();
        Map<String, Object> mapResult= resttemplate.SendMessage(map,ConstantInterface.port + "/HSDC/authcode/Autherized");
        return mapResult;
    }

}
