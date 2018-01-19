package com.reptile.service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
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
     * @return
     */
    public Map<String, Object> getDetailMes(HttpServletRequest request, String IDNumber, String cardNumber, String userName, String passWord, String UUID) {
        Map<String, Object> map = new HashMap<String, Object>();
        PushSocket.pushnew(map, UUID, "1000", "登录中信银行网上银行");
        PushState.state(IDNumber, "savings", 100);
        System.setProperty("webdriver.ie.driver", "C:/ie/IEDriverServer.exe");
        InternetExplorerDriver driver = new InternetExplorerDriver();
        driver.manage().window().maximize();
        try {
            logger.warn("登录中信银行网上银行");
            driver.get("https://i.bank.ecitic.com/perbank6/signIn.do");
            Thread.sleep(1000);
            //输入账户名密码
            driver.findElementByName("logonNoCert").sendKeys(cardNumber);
//            Actions actions = new Actions(driver);
//            actions.sendKeys(Keys.TAB).build().perform();
//            Thread.sleep(1000);
//            for (int i = 0; i < passWord.length(); i++) {
//                VirtualKeyBoard.KeyPress(passWord.charAt(i));
//                Thread.sleep(200);
//            }
            SendKeys.sendStr(1400, 250, passWord);
//            SendKeys.sendStr(1422, 322, passWord);//本地弹框

//            SendKeys.sendTab();
//            Thread.sleep(1000);
//			 SendKeys.sendStr(passWord);
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
                PushState.state(IDNumber, "savings", 200, errorReason.getText());
                driver.quit();
                return map;
            } catch (NoSuchElementException e) {
                logger.warn("登录成功");
            } catch (UnhandledAlertException e) {
                map.put("errorCode", "0002");
                map.put("errorInfo", "账号或密码格式不正确！");
                PushSocket.pushnew(map, UUID, "3000", "中信银行登陆失败,账号或密码格式不正确！");
                PushState.state(IDNumber, "savings", 200, "中信银行登陆失败,账号或密码格式不正确！");
                driver.quit();
                return map;
            }
//            PushSocket.push(map, UUID, "0000");
            PushSocket.pushnew(map, UUID, "2000", "中信银行登陆成功");
            logger.warn("获取账单详情...");
            //获取类似于tooken的标识
            PushSocket.pushnew(map, UUID, "5000", "中信银行信息获取中");
            String EMP_SID = driver.findElementByName("infobarForm").findElement(By.name("EMP_SID")).getAttribute("value");
            //获取账单详情
            List<String> dataList = new ArrayList<String>();
            String baseMes = "";
            try {
                dataList = getItemMes(driver, EMP_SID, dataList);
                map.put("itemMes", dataList);
                logger.warn("获取账单详情成功");
                logger.warn("获取基本信息");
                //发包获取基本信息
                driver.get("https://i.bank.ecitic.com/perbank6/pb1110_query_detail.do?EMP_SID=" + EMP_SID + "&accountNo=" + cardNumber + "&index=0ff0 ");
                Thread.sleep(2000);
                baseMes = driver.getPageSource();
            } catch (Exception e) {
                map.put("errorCode", "0002");
                map.put("errorInfo", "获取账单过程中出现异常，请重试！");
                PushSocket.pushnew(map, UUID, "7000", "中信银行信息获取失败");
                PushState.state(IDNumber, "savings", 200, "中信银行信息获取失败");
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

            logger.warn("中信银行数据推送...");
            PushSocket.pushnew(map, UUID, "6000", "中信银行信息获取成功");
            //推送数据
            map = new Resttemplate().SendMessage(map, ConstantInterface.port + "/HSDC/savings/authentication");
            if (map != null && "0000".equals(map.get("errorCode").toString())) {
                map.put("errorInfo", "查询成功");
                map.put("errorCode", "0000");
                PushSocket.pushnew(map, UUID, "8000", "中信银行认证成功");
                PushState.state(IDNumber, "savings", 300);
            } else {
                //--------------------数据中心推送状态----------------------
                PushSocket.pushnew(map, UUID, "9000", map.get("errorInfo").toString());
                PushState.state(IDNumber, "savings", 200, map.get("errorInfo").toString());
                //---------------------数据中心推送状态----------------------

            }
            logger.warn("中信银行数据推送成功");
            driver.quit();//关闭浏览器
        } catch (Exception e) {
            driver.quit();
            logger.warn("中信银行认证失败", e);
            PushSocket.pushnew(map, UUID, "7000", "中信银行信息获取失败");
            PushState.state(IDNumber, "savings", 200, "中信银行信息获取失败");
            map.clear();
            map.put("errorCode", "0001");
            map.put("errorInfo", "网络请求异常，请稍后再试");
        }
        return map;
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
        //移除时间输入框的readOnly属性
        driver.executeScript("document.getElementById('beginDate').removeAttribute('readonly');document.getElementById('endDate').removeAttribute('readonly');");
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
            String s = itemMes.get(index).replaceAll("tbody", "table");
            Document parse = Jsoup.parse(s);
            Elements tbody = parse.getElementsByTag("table");
            Elements tr = tbody.get(0).getElementsByTag("tr");
            for (int i = 0; i < tr.size(); i++) {
                Elements td = tr.get(i).getElementsByTag("td");
                detailMap = new HashMap<String, Object>();
                detailMap.put("dealTime", td.get(2).text());
                detailMap.put("expendMoney", td.get(3).text());
                detailMap.put("incomeMoney", td.get(4).text());
                detailMap.put("balanceAmount", td.get(5).text());
                detailMap.put("oppositeSideName", td.get(6).text());
                detailMap.put("dealDitch", td.get(7).text());
                detailMap.put("dealReferral", td.get(8).text());
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
}
