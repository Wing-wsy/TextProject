/**
 * 版权所有：版权所有(C) 2016，远行科技
 * 文件编号：M01_NotifiesController.java
 * 文件名称：NotifiesController.java
 * 系统编号：远行福利plus
 * 设计作者：彭龙
 * 完成日期：2016年7月20日
 * 设计文档：
 * 内容摘要：TODO
 * 系统用例：
 * 界面原型：
 */
package com.vispractice.vpshop.controller.shop.pay;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import me.hao0.wepay.util.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Throwables;
import com.vispractice.alipay.model.AlipayFields;
import com.vispractice.alipay.model.enums.AlipayField;
import com.vispractice.alipay.model.enums.TradeStatus;
import com.vispractice.vpshop.facade.WechatPayFacadeService;
import com.vispractice.vpshop.plugin.AlipaySupport;
import com.vispractice.vpshop.plugin.WepaySupport;
import com.vispractice.vpshop.util.XmlUtil;

/**
 * 类 编 号：UI_PU010401_NotifiesController
 * 类 名 称：NotifiesController.java
 * 内容摘要：微信支付回调通知
 * 完成日期：2016年8月24日 上午10:57:07
 * 编码作者: 彭龙
 */
@Controller("notifiesController")
@RequestMapping("/notifies")
public class NotifiesController {

    private static final Logger logger = LoggerFactory.getLogger(NotifiesController.class);

    @Autowired
    private WepaySupport wepaySupport;
    @Autowired
    private AlipaySupport alipaySupport;
    
    @Resource(name = "wechatPayFacadeServiceImpl")
   	private WechatPayFacadeService wechatPayFacadeService;

    /**
     * 方法:支付成功通知
     * 实现流程:
     *    1.校验签名是否正确
     *    2.处理相关订单信息
     * autor :彭龙
     * @param request请求对象
     * @return 处理结果
     */
    @RequestMapping(value ="/paid", method = RequestMethod.POST)
    public @ResponseBody String paid(HttpServletRequest request){

        String notifyXml = getPostRequestBody(request);
        //回调内容为空
        if (notifyXml.isEmpty()){
            return wepaySupport.notifyNotOk("body为空");
        }

        Map<String, Object> notifyParams = XmlUtil.xmlToMap(notifyXml);
        
        //校验回调签名
        if (wepaySupport.verifySign(notifyParams)){

        	 logger.info("verify sign success: {}", notifyParams);
            // TODO business logic
        	Boolean flag = wechatPayFacadeService.handleWechatNotify(notifyParams);
        	if(flag)
        	{
              return wepaySupport.notifyOk();
        	}
        	else{
        		return wepaySupport.notifyNotOk("签名失败");
        	}
        } else {

            logger.error("verify sign failed: {}", notifyParams);
            return wepaySupport.notifyNotOk("签名失败");
        }
    }
    
    /**
     * 方法:支付宝支付成功通知
     * 实现流程:
     *    1.处理相关订单信息
     * autor :彭龙
     * @param request请求对象
     * @return 处理结果
     */
    @RequestMapping("/backend")
    @ResponseBody
    public String backend(HttpServletRequest request){
        Map<String, String> notifyParams = new HashMap<>();

        // TODO 这里还是建议直接从request中获取map参数，兼容支付宝修改或增减参数
        for (AlipayField f : AlipayFields.WEB_PAY_NOTIFY){
            notifyParams.put(f.field(), request.getParameter(f.field()));
        }
        logger.info("backend notify params: {}", notifyParams);
        if (!alipaySupport.notifyVerifyMd5(notifyParams)){
            logger.error("backend sign verify failed");
            return "fail";
        }

        String tradeStatus = notifyParams.get(AlipayField.TRADE_STATUS.field());
        if (TradeStatus.TRADE_FINISHED.value().equals(tradeStatus)
                || TradeStatus.TRADE_SUCCESS.value().equals(tradeStatus)){
            // 交易成功
        	Boolean flag = wechatPayFacadeService.handleAlipayNotify(notifyParams);
        	if(flag)
        	{
              return "success";
        	}
        	else{
        		return "fail";
        	}
        }

        logger.info("backend notify success");
        return "success";
    }
 
    /**
     * 退款通知
     */
    @RequestMapping("/refund")
    @ResponseBody
    public String refund(HttpServletRequest request){
        Map<String, String> receives = new HashMap<>();

        // TODO 这里还是建议直接从request中获取map参数，兼容支付宝修改或增减参数
        for (AlipayField f : AlipayFields.REFUND_NOTIFY){
            receives.put(f.field(), request.getParameter(f.field()));
        }

        logger.info("refund notify params: {}", receives);
        if (!alipaySupport.notifyVerifyMd5(receives)){
            System.out.println("refund sign verify failed");
            return "FAIL";
        }

        logger.info("refund notify success");
        return "success";
    }

    /**
     * 方法:获取返回http内容
     * 实现流程:
     * autor :彭龙
     * @param req
     * @return
     */
    public static String getPostRequestBody(HttpServletRequest req) {
        if (req.getMethod().equals("POST")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = req.getReader()) {
                char[] charBuffer = new char[128];
                int bytesRead;
                while ((bytesRead = br.read(charBuffer)) != -1) {
                    sb.append(charBuffer, 0, bytesRead);
                }
            } catch (IOException e) {
                logger.warn("failed to read request body, cause: {}", Throwables.getStackTraceAsString(e));
            }
            return sb.toString();
        }
        return "";
    }
}
