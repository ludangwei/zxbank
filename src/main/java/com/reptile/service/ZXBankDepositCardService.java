package com.reptile.service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hoomsun.keyBoard.SendKeys;
import com.reptile.util.ConstantInterface;
import com.reptile.util.PushSocket;
import com.reptile.util.PushState;
import com.reptile.util.Resttemplate;
import com.reptile.util.RobotUntil;
import com.reptile.util.SimpleHttpClient;

@Service
public class ZXBankDepositCardService {
    private Logger logger = LoggerFactory.getLogger(ZXBankDepositCardService.class);

    /**
     * 获取中信银行储蓄卡账单信息
     *
     * @param request
     * @param IDNumber
     * @param cardNumber
     * @param userName
     * @param passWord
     * @param flag 
     * @return
     */
    public Map<String, Object> getDetailMes(HttpServletRequest request, String IDNumber, String cardNumber, String userName, String passWord, String UUID, boolean flag) {
        Map<String, Object> map = new HashMap<String, Object>();
        PushSocket.pushnew(map, UUID, "1000", "登录中信银行网上银行");
        PushState.stateByFlag(IDNumber, "savings", 100,flag);
        System.setProperty("java.awt.headless", "false");
        System.setProperty("webdriver.ie.driver", "C:/ie/IEDriverServer.exe");
        InternetExplorerDriver driver = new InternetExplorerDriver();
        driver.manage().window().maximize();
        try {
            logger.warn("登录中信银行网上银行");
            driver.get("https://i.bank.ecitic.com/perbank6/signIn.do");
            Thread.sleep(1000);
            //输入账户名密码
            driver.findElementByName("logonNoCert").sendKeys(cardNumber);
            //Map<String,Integer> ss = Image.findImageFullScreen("C:\\searchImg\\screen.png", "C:\\searchImg\\search.png", "C:\\searchImg\\rest.png");
            //SendKeys.sendStr(ss.get("x")+100, ss.get("y"), passWord);
             SendKeys.sendStr(1414, 314, passWord);//正式
             // SendKeys.sendStr(1400, 250, passWord);
            //判断是否存在验证码
            try {
                WebElement pinImg = driver.findElementById("pinImg");
                String realPath = request.getServletContext().getRealPath("/verImageCode");
                File file = new File(realPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                driver.findElementByClassName("loginInputVerity").sendKeys("123");
                driver.findElementByClassName("loginInputVerity").clear();
                String code = RobotUntil.getImgFileByScreenshot(pinImg, driver, file);
                logger.warn("中信银行出现验证码");
                driver.findElementByClassName("loginInputVerity").sendKeys(code);
            } catch (Exception e) {
                logger.warn("中信银行未出现验证码", e);
            }
            //登录
            driver.findElementById("logonButton").click();
            Thread.sleep(3000);
            //判断是否登录成功
            try {
                WebElement errorReason = driver.findElementByClassName("errorReason");
                logger.warn("登录失败！" + errorReason.getText());
                map.put("errorCode", "0002");
                map.put("errorInfo", errorReason.getText());
                PushSocket.pushnew(map, UUID, "3000", "中信银行登陆失败," + errorReason.getText());
                PushState.stateByFlag(IDNumber, "savings", 200, errorReason.getText(),flag);
                driver.quit();
                return map;
            } catch (NoSuchElementException e) {
                logger.warn("登录成功");
            } catch (UnhandledAlertException e) {
                map.put("errorCode", "0002");
                map.put("errorInfo", "账号或密码格式不正确！");
                PushSocket.pushnew(map, UUID, "3000", "中信银行登陆失败,账号或密码格式不正确！");
                PushState.stateByFlag(IDNumber, "savings", 200, "中信银行登陆失败,账号或密码格式不正确！",flag);
                driver.quit();
                return map;
            }
//            PushSocket.push(map, UUID, "0000");
            PushSocket.pushnew(map, UUID, "2000", "中信银行登陆成功");
            logger.warn("获取账单详情...");
            //获取类似于tooken的标识
            PushSocket.pushnew(map, UUID, "5000", "中信银行信息获取中");
            String emp_sid = driver.findElementByName("infobarForm").findElement(By.name("EMP_SID")).getAttribute("value");
            //获取账单详情
            List<String> dataList = new ArrayList<String>();
            String baseMes = "";
            try {
                //----------------原方法---------------------------
               /* dataList = getItemMes(driver, emp_sid, dataList);
                map.put("itemMes", dataList);
            	logger.warn("获取基本信息");
            	//发包获取基本信息
            	driver.get("https://i.bank.ecitic.com/perbank6/pb1110_query_detail.do?EMP_SID=" + emp_sid + "&accountNo=" + cardNumber + "&index=0ff0 ");
                Thread.sleep(2000);
                baseMes = driver.getPageSource();
                System.out.println("baseMes==="+baseMes);*/
                //----------------原方法---------------------------

                //-----------------发包获取账单详情------------------
                Set<Cookie> ingoCookie = driver.manage().getCookies();
                StringBuffer cookies=new StringBuffer();
                for (Cookie co:ingoCookie) {
                    cookies.append(co.getName()+"="+co.getValue()+";");
                }
                String cookie = cookies.toString();
                driver.quit();
                logger.warn("发包获取账单"+emp_sid+cardNumber);
                dataList = getItemMesfb(emp_sid, dataList,cardNumber,cookie);
                map.put("itemMes", dataList);

                logger.warn("发包获取基本信息");
                 Map<String,String> headers=new HashMap<String, String>();
                headers.put("Accept", "*/*");
                headers.put("Accept-Encoding", "gzip, deflate");
                headers.put("Accept-Language", "zh-CN");
                headers.put("Cache-Control", "no-cache");
                headers.put("Connection", "Keep-Alive");
                headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                headers.put("Cookie", cookie);
                headers.put("Host", "i.bank.ecitic.com");
                headers.put("Referer", "https://i.bank.ecitic.com/perbank6/pb1110_subaccount_detail.do?EMP_SID="+emp_sid+"&accountNo="+cardNumber+"&cardType=0&recordNum=0&totalNum=0");
                headers.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; "
                        + ".NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET4.0C; .NET4.0E)");
                headers.put("x-requested-with", "XMLHttpRequest");
                String url="https://i.bank.ecitic.com/perbank6/pb1110_query_detail.do?EMP_SID=" + emp_sid + "&accountNo=" + cardNumber + "&index=0ff0";
                baseMes=SimpleHttpClient.get(url,headers);
                logger.warn("获取账单详情成功");
            } catch (Exception e) {
                e.printStackTrace();
                map.put("errorCode", "0002");
                map.put("errorInfo", "获取账单过程中出现异常，请重试！");
                PushSocket.pushnew(map, UUID, "7000", "中信银行信息获取失败");
                PushState.stateByFlag(IDNumber, "savings", 200, "中信银行信息获取失败",flag);
                driver.quit();
                return map;
            }
            Thread.sleep(1000);

            map.put("baseMes", baseMes);
            map = analyData(map);
            map.put("IDNumber", IDNumber);
            map.put("cardNumber", cardNumber);
            map.put("userName", userName);
            map.put("bankName", "中信银行");
            map.put("userAccount", cardNumber);
            System.out.println("我的结果=="+map);
            logger.warn("中信银行数据推送...");
            PushSocket.pushnew(map, UUID, "6000", "中信银行信息获取成功");
            //推送数据
            map = new Resttemplate().SendMessage(map, ConstantInterface.port + "/HSDC/savings/authentication");
            if (map != null && "0000".equals(map.get("errorCode").toString())) {
                map.put("errorInfo", "查询成功");
                map.put("errorCode", "0000");
                PushSocket.pushnew(map, UUID, "8000", "中信银行认证成功");
                PushState.stateByFlag(IDNumber, "savings", 300,flag);
            } else {
                //--------------------数据中心推送状态----------------------
                PushSocket.pushnew(map, UUID, "9000", "中信银行认证失败" + map.get("errorInfo").toString());
                PushState.stateByFlag(IDNumber, "savings", 200, map.get("errorInfo").toString(),flag);
                //---------------------数据中心推送状态----------------------

            }
            logger.warn("中信银行数据推送成功");
            driver.quit();//关闭浏览器
        } catch (Exception e) {
            driver.quit();
            logger.warn("中信银行认证失败", e);
            PushSocket.pushnew(map, UUID, "7000", "中信银行信息获取失败");
            PushState.stateByFlag(IDNumber, "savings", 200, "中信银行信息获取失败",flag);
            map.clear();
            map.put("errorCode", "0001");
            map.put("errorInfo", "网络请求异常，请稍后再试");
        }
        return map;
    }


    private List<String> getItemMesfb(String EMP_SID, List<String> dataList,String cardNumber,String cookie) {
        SimpleDateFormat sim = new SimpleDateFormat("yyyyMMdd");
        Calendar cal = Calendar.getInstance();
        String endTime = sim.format(cal.getTime());
        String year=endTime.substring(0, 4);
        int year1=Integer.parseInt(year);
        String nowMonth=endTime.substring(4, 6);
        int nowMonth1=Integer.parseInt(nowMonth);
        String beginTime=beforMonth(year1, nowMonth1, 6);
        try {

            //第一次发包开始
            String url11="https://i.bank.ecitic.com/perbank6/trans_3063s.do?EMP_SID="+EMP_SID;
            Map<String,Object> params11=new HashMap<String, Object>();
            params11.put("accountNo", cardNumber);
            params11.put("selectSubAccount", "null");

            Map<String,String> headers11=new HashMap<String, String>();
            headers11.put("Accept", "*/*");
            headers11.put("Accept-Encoding", "gzip, deflate");
            headers11.put("Accept-Language", "zh-CN");
            headers11.put("Cache-Control", "no-cache");
            headers11.put("Connection", "Keep-Alive");
            headers11.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            headers11.put("Cookie", cookie);
            headers11.put("Host", "i.bank.ecitic.com");
            headers11.put("Referer", "https://i.bank.ecitic.com/perbank6/pb1310_account_detail_query.do?EMP_SID="+EMP_SID);
            headers11.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; "
                    + ".NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET4.0C; .NET4.0E)");

            String response11=SimpleHttpClient.post(url11,params11, headers11);
            
            
            Document parse = Jsoup.parse(response11);
            logger.warn("第一次发包数据结果"+parse);
            Elements isubAccInfoaccountNo0 = parse.getElementsByAttributeValue("name", "isubAccInfo.accountNo");
            String isubAccInfoaccountNo = isubAccInfoaccountNo0.get(0).attr("value");

            Elements isubAccInforecordState0 = parse.getElementsByAttributeValue("name", "isubAccInfo.recordState");
            String isubAccInforecordState= isubAccInforecordState0.get(0).attr("value");

            Elements isubAccInfoopenDate0 = parse.getElementsByAttributeValue("name", "isubAccInfo.openDate");
            String isubAccInfoopenDate= isubAccInfoopenDate0.get(0).attr("value");

            Elements isubAccInfoqcAmt0 = parse.getElementsByAttributeValue("name", "isubAccInfo.qcAmt");
            String isubAccInfoqcAmt= isubAccInfoqcAmt0.get(0).attr("value");

            Elements isubAccInfocreditBalance0 = parse.getElementsByAttributeValue("name", "isubAccInfo.creditBalance");
            String isubAccInfocreditBalance= isubAccInfocreditBalance0.get(0).attr("value");

            Elements isubAccInfoextendFlag0 = parse.getElementsByAttributeValue("name", "isubAccInfo.extendFlag");
            String isubAccInfoextendFlag= isubAccInfoextendFlag0.get(0).attr("value");

            Elements isubAccInfosubAccountNo0 = parse.getElementsByAttributeValue("name", "isubAccInfo.subAccountNo");
            String isubAccInfosubAccountNo= isubAccInfosubAccountNo0.get(0).attr("value");

            Elements isubAccInfofrozenSFlag0 = parse.getElementsByAttributeValue("name", "isubAccInfo.frozenSFlag");
            String isubAccInfofrozenSFlag= isubAccInfofrozenSFlag0.get(0).attr("value");

            Elements isubAccInfolossFlag0 = parse.getElementsByAttributeValue("name", "isubAccInfo.lossFlag");
            String isubAccInfolossFlag= isubAccInfolossFlag0.get(0).attr("value");

            Elements isubAccInfobalance0 = parse.getElementsByAttributeValue("name", "isubAccInfo.balance");
            String isubAccInfobalance= isubAccInfobalance0.get(0).attr("value");

            Elements isubAccInfohostAccType0 = parse.getElementsByAttributeValue("name", "isubAccInfo.hostAccType");
            String isubAccInfohostAccType= isubAccInfohostAccType0.get(0).attr("value");

            Elements isubAccInfosavePeriod0 = parse.getElementsByAttributeValue("name", "isubAccInfo.savePeriod");
            String isubAccInfosavePeriod= isubAccInfosavePeriod0.get(0).attr("value");

            Elements isubAccInfooriginalBalance0 = parse.getElementsByAttributeValue("name", "isubAccInfo.originalBalance");
            String isubAccInfooriginalBalance= isubAccInfooriginalBalance0.get(0).attr("value");

            Elements isubAccInfosaveType0 = parse.getElementsByAttributeValue("name", "isubAccInfo.saveType");
            String isubAccInfosaveType= isubAccInfosaveType0.get(0).attr("value");

            Elements isubAccInfojurForzenAmt0 = parse.getElementsByAttributeValue("name", "isubAccInfo.jurForzenAmt");
            String isubAccInfojurForzenAmt= isubAccInfojurForzenAmt0.get(0).attr("value");

            Elements isubAccInfojudFrozenSFlag0 = parse.getElementsByAttributeValue("name", "isubAccInfo.judFrozenSFlag");
            String isubAccInfojudFrozenSFlag= isubAccInfojudFrozenSFlag0.get(0).attr("value");

            Elements isubAccInfointerestRate0 = parse.getElementsByAttributeValue("name", "isubAccInfo.interestRate");
            String isubAccInfointerestRate= isubAccInfointerestRate0.get(0).attr("value");

            Elements isubAccInfoforzenAmt0 = parse.getElementsByAttributeValue("name", "isubAccInfo.forzenAmt");
            String isubAccInfoforzenAmt= isubAccInfoforzenAmt0.get(0).attr("value");

            Elements isubAccInfocurrencyType0 = parse.getElementsByAttributeValue("name", "isubAccInfo.currencyType");
            String isubAccInfocurrencyType= isubAccInfocurrencyType0.get(0).attr("value");

            Elements isubAccInfoaccrualStartDate0 = parse.getElementsByAttributeValue("name", "isubAccInfo.accrualStartDate");
            String isubAccInfoaccrualStartDate= isubAccInfoaccrualStartDate0.get(0).attr("value");

            Elements isubAccInfoaccrualEDate0 = parse.getElementsByAttributeValue("name", "isubAccInfo.accrualEDate");
            String isubAccInfoaccrualEDate= isubAccInfoaccrualEDate0.get(0).attr("value");

            Elements selectid0 = parse.getElementsByTag("a");
            String selectid= selectid0.get(0).attr("selectid");

            //第二次发包开始
            String url="https://i.bank.ecitic.com/perbank6/pb1310_account_detail.do?EMP_SID="+EMP_SID;
            Map<String,Object> params=new HashMap<String, Object>();
            params.put("accountNo", cardNumber);
            params.put("beforePageparams", "");
            params.put("beginAmt", "");
            params.put("beginAmtText", "请输入起始金额");
            params.put("beginDate", beginTime);
            params.put("CashFlag", "");
            params.put("currList", selectid);
            params.put("endAmt", "");
            params.put("endAmtText", "请输入截止金额");
            params.put("endDate", endTime);
            params.put("isubAccInfo.accountNo", isubAccInfoaccountNo);
            params.put("isubAccInfo.accrualEDate", isubAccInfoaccrualEDate);
            params.put("isubAccInfo.accrualStartDate", isubAccInfoaccrualStartDate);
            params.put("isubAccInfo.balance", isubAccInfobalance);
            params.put("isubAccInfo.creditBalance", isubAccInfocreditBalance);
            params.put("isubAccInfo.currencyType", isubAccInfocurrencyType);
            params.put("isubAccInfo.extendFlag", isubAccInfoextendFlag);
            params.put("isubAccInfo.forzenAmt", isubAccInfoforzenAmt);
            params.put("isubAccInfo.frozenSFlag", isubAccInfofrozenSFlag);
            params.put("isubAccInfo.hostAccType",isubAccInfohostAccType);
            params.put("isubAccInfo.interestRate", isubAccInfointerestRate);
            params.put("isubAccInfo.judFrozenSFlag", isubAccInfojudFrozenSFlag);
            params.put("isubAccInfo.jurForzenAmt", isubAccInfojurForzenAmt);
            params.put("isubAccInfo.lossFlag", isubAccInfolossFlag);
            params.put("isubAccInfo.openDate", isubAccInfoopenDate);
            params.put("isubAccInfo.originalBalance", isubAccInfooriginalBalance);
            params.put("isubAccInfo.qcAmt", isubAccInfoqcAmt);
            params.put("isubAccInfo.recordState", isubAccInforecordState);
            params.put("isubAccInfo.savePeriod", isubAccInfosavePeriod);
            params.put("isubAccInfo.saveType", isubAccInfosaveType);
            params.put("isubAccInfo.subAccountNo", isubAccInfosubAccountNo);
            params.put("largeAmount", "");
            params.put("opFlag", "1");
            params.put("pageType", "1");
            params.put("payAcctxt", cardNumber);
            params.put("queryDays", "");
            params.put("queryType", "spacil");
            params.put("recordNum", "99");
            params.put("recordSize", "200");
            params.put("recordStart", "1");
            params.put("startPageFlag", "");
            params.put("std400chnn", "");
            params.put("std400dcfg", "");
            params.put("std400pgqf", "N");
            params.put("std400pgtk", "");
            params.put("std400pgts", "");
            params.put("stdessbgdt", beginTime);
            params.put("stdesseddt", endTime);
            params.put("stdesssbno", "");
            params.put("stdpriacno", isubAccInfoaccountNo);
            params.put("stdudfcyno", "001");
            params.put("stkessmnam", "");
            params.put("targetPage", "1");

            Map<String,String> headers=new HashMap<String, String>();
            headers.put("Accept", "*/*");
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Accept-Language", "zh-CN");
            headers.put("Cache-Control", "no-cache");
            headers.put("Connection", "Keep-Alive");
            headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            headers.put("Cookie", cookie);
            headers.put("Host", "i.bank.ecitic.com");
            headers.put("Referer", "https://i.bank.ecitic.com/perbank6/pb1310_account_detail_query.do?EMP_SID="+EMP_SID);
            headers.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; "
                    + ".NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET4.0C; .NET4.0E)");

            String response=SimpleHttpClient.post(url,params, headers);
            Document parse1 = Jsoup.parse(response);
            Elements body = parse1.getElementsByTag("tbody");
            dataList.add(body.toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return dataList;
    }

    /**
     * 获取账单详情
     *
     * @param driver
     * @param EMP_SID
     * @param dataList
     * @return
     * @throws Exception
     */
    public List<String> getItemMes(InternetExplorerDriver driver, String EMP_SID, List<String> dataList) throws Exception {
        //将mainframe切换到详单页面

        driver.executeScript("document.getElementById(\"mainframe\").src=\"https://i.bank.ecitic.com/perbank6/pb1310_account_detail_query.do?EMP_SID=" + EMP_SID + "\" ");
        Thread.sleep(2000);
        WebElement mainframe = driver.findElementById("mainframe");
        driver.switchTo().frame(mainframe);
        Thread.sleep(1000);
        //打开自定义查询
        driver.findElement(By.id("spacilOpenDiv")).click();
        Thread.sleep(1000);
        //移除时间输入框的readOnly属性
        driver.executeScript("document.getElementById('beginDate').removeAttribute('readonly');document.getElementById('endDate').removeAttribute('readonly');");
        Thread.sleep(1000);
        driver.findElement(By.id("beginDate")).clear();
        driver.findElement(By.id("endDate")).clear();
        //查询开始时间，结束时间（当前时间前两天）设置
        SimpleDateFormat sim = new SimpleDateFormat("yyyyMMdd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -2);
        String endTime = sim.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String beginTime = sim.format(cal.getTime());
        //循环获取6个月的账单信息
        for (int i = 0; i < 6; i++) {
            driver.findElement(By.id("beginDate")).clear();
            driver.findElement(By.id("endDate")).clear();
            driver.findElement(By.id("beginDate")).sendKeys(beginTime);
            driver.findElement(By.id("endDate")).sendKeys(endTime);
            driver.findElementById("searchButton").click();
            Thread.sleep(1000);

            String attribute = driver.findElementById("resultTable1").getAttribute("innerHTML");
            dataList.add(attribute);
            //上月末
            cal.add(Calendar.DAY_OF_MONTH, -1);
            endTime = sim.format(cal.getTime());
            //上月初
            cal.set(Calendar.DAY_OF_MONTH, 1);
            beginTime = sim.format(cal.getTime());
        }

        return dataList;
    }

    /**
     * 解析从页面获取到的数据并封装
     *
     * @param
     * @return
     */
    private Map<String, Object> analyData(Map<String, Object> paramMap) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        List<String> itemMes = (List<String>) paramMap.get("itemMes");  //账单信息
        String baseMes = paramMap.get("baseMes").toString();            //基本信息

        if (itemMes.size() == 0) {
            map.put("errorCode", "1001");
            map.put("errorInfo", "账单信息为空");
            return map;
        }
        List billList;                                                   //解析后账单信息
        Map<String, Object> baseMap;                             //解析后基本信息
        try {
            billList = analyBillMethod(itemMes);                         //解析账单信息
            baseMap = analyBaseMes(baseMes);                     //解析基本信息
        } catch (Exception e) {
            logger.warn("数据解析失败", e);
            throw new Exception("数据解析失败");
        }
        map.put("baseMes", baseMap);
        map.put("billMes", billList);
        return map;
    }


    /**
     * 解析账单信息
     *
     * @param itemMes
     * @return
     */
    private List analyBillMethod(List<String> itemMes) throws Exception {
        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        Map<String, Object> detailMap;  //存放当月的交易详情

        for (int index = 0; index < itemMes.size(); index++) {
            String s = itemMes.get(index).replaceAll("TBODY", "table").replaceAll("tbody", "table");
            logger.warn("itemMes===="+itemMes);
            logger.warn("s===="+s);
            Document parse = Jsoup.parse(s);
            Elements tbody = parse.getElementsByTag("table");
            logger.warn("tbody===="+tbody);
            Elements tr = tbody.get(0).getElementsByTag("tr");
            for (int i = 0; i < tr.size(); i++) {
                Elements td = tr.get(i).getElementsByTag("td");
                detailMap = new HashMap<String, Object>();
                detailMap.put("dealTime", td.get(2).text().replace(" ", ""));
                detailMap.put("expendMoney", td.get(3).text().replace("  ", ""));
                detailMap.put("incomeMoney", td.get(4).text().replace("  ", ""));
                detailMap.put("balanceAmount", td.get(5).text().replace(" ", ""));
                detailMap.put("oppositeSideName", td.get(6).text());
                detailMap.put("dealDitch", td.get(7).text().replace(" ", ""));
                detailMap.put("dealReferral", td.get(8).text().replace(" ", ""));
                dataList.add(detailMap);
            }

        }
        return dataList;
    }


    /**
     * 解析账单信息
     *
     * @param itemMes
     * @return
     */
    private List analyBillMethodfb(List<String> itemMes) throws Exception {
        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        Map<String, Object> detailMap;  //存放当月的交易详情

        for (int index = 0; index < itemMes.size(); index++) {
        	System.out.println();
            String s = itemMes.get(index).replaceAll("tbody", "table");
            Document parse = Jsoup.parse(s);
            System.out.println("parse==="+parse);
            Elements tbody = parse.getElementsByTag("table");
            Elements tr = tbody.get(0).getElementsByTag("tr");
            for (int i = 0; i < tr.size(); i++) {
                Elements td = tr.get(i).getElementsByTag("td");
                detailMap = new HashMap<String, Object>();
                detailMap.put("dealTime", td.get(2).text().replace(" ", ""));
                detailMap.put("expendMoney", td.get(3).text().replace("  ", ""));
                detailMap.put("incomeMoney", td.get(4).text().replace("  ", ""));
                detailMap.put("balanceAmount", td.get(5).text().replace(" ", ""));
                detailMap.put("oppositeSideName", td.get(6).text());
                detailMap.put("dealDitch", td.get(7).text().replace(" ", ""));
                detailMap.put("dealReferral", td.get(8).text().replace(" ", ""));
                dataList.add(detailMap);
            }

        }
        return dataList;
    }

    /**
     * 解析基本信息
     *
     * @param baseMes
     * @return
     */
    private Map<String, Object> analyBaseMes(String baseMes) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        Document parse = Jsoup.parse(baseMes);
        Elements tbody = parse.getElementsByTag("tbody");
        Elements td = tbody.get(0).getElementsByTag("td");

        map.put("accountType", td.get(9).text());
        map.put("openBranch", "");
        map.put("openTime", td.get(5).text());
        return map;
    }
    /**
     * 获得yyyy/MM格式的上个num个月
     * @param year
     * @param month
     * @return
     */

    public static  String beforMonth(int year,int nowMonth,int num ){
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf =  new SimpleDateFormat( "yyyyMMdd" );
        cal.set(Calendar.YEAR,year);
        cal.set(Calendar.MONTH, nowMonth-1);
        cal.add(Calendar.MONTH, -num);//从现在算，之前一个月,如果是2个月，那么-1-----》改为-2
        return sdf.format(cal.getTime());

    }
}
