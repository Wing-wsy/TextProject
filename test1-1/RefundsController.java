/*package com.vispractice.vpshop.controller.shop.pay;

import me.hao0.wepay.model.refund.RefundApplyResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vispractice.vpshop.plugin.WepaySupport;

*//**
 * Author: penglong
 * Email: jxufepenglong@163.com
 * Date: 3/12/15
 *//*
@Controller("refundsController")
@RequestMapping("/refunds")
public class RefundsController {

    @Autowired
    private WepaySupport wepaySupport;

    *//**
     * 退款申请
     * @param orderNumber 商户订单号
     *//*
    @RequestMapping("/apply")
    @ResponseBody
    public RefundApplyResponse apply(@RequestParam("sn") String sn){
        return wepaySupport.refundApply(sn, "2016082449999", 100, 99);
    }
}*/
