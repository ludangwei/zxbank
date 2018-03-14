
package com.reptile.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reptile.service.ZXBankDepositCardService;
/**
 * 信用卡判断是否推送100，200，300
 * @ClassName: CountTime  
 * @Description: TODO  
 * @author: fangshuang
 * @date 2017年12月29日  
 *
 */
public class CountTime {
	public static boolean getCountTime(String timeCnt) throws ParseException {
		Logger logger = LoggerFactory.getLogger(ZXBankDepositCardService.class);
		logger.warn("*****timeCnt*****认证时间*****"+timeCnt);
		Date newDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date cntDate = sdf.parse(timeCnt);
		int days = (int) ((newDate.getTime()-cntDate.getTime())/(1000*60*60*24));
		final int countday = 30;
		if(days>countday) {
			return true;
		}else {
			return false;
		}		
	}
}

