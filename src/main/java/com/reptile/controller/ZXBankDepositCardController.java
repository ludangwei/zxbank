package com.reptile.controller;


import com.reptile.service.ZXBankDepositCardService;
import com.reptile.util.CustomAnnotation;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 中信银行储蓄卡
 */

@Controller
@RequestMapping("ZXBankDepositCardController")
public class ZXBankDepositCardController {
    @Autowired
    private ZXBankDepositCardService service;

    @RequestMapping(value = "getDetailMes", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value = "储蓄卡获取数据", notes = "参数：身份证，卡号，用户名，密码,uuid")
//    @CustomAnnotation
    public Map<String, Object> getDetailMes(HttpServletRequest request, @RequestParam("IDNumber") String IDNumber,
                                            @RequestParam("cardNumber") String cardNumber, @RequestParam("passWord") String passWord,
                                            @RequestParam("userName") String userName, @RequestParam("UUID") String UUID) {
        Map<String, Object> detailMes;
        synchronized (this) {
            detailMes = service.getDetailMes(request, IDNumber, cardNumber, userName, passWord, UUID);
        }
        return detailMes;
    }
}
