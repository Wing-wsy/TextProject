/**
 * 版权所有：版权所有(C) 2016，远行科技
 * 文件编号：M01_PaysController.java
 * 文件名称：PaysController.java
 * 系统编号：远行福利plus
 * 设计作者：彭龙
 * 完成日期：2016年7月20日
 * 设计文档：
 * 内容摘要：TODO
 * 系统用例：
 * 界面原型：
 */
package com.vispractice.vpshop.controller.shop.pay;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vispractice.vpshop.CommonAttributes;
import com.vispractice.vpshop.Message;
import com.vispractice.vpshop.agent.service.AgentCompanyService;
import com.vispractice.vpshop.constant.Constants;
import com.vispractice.vpshop.dao.JedisDao;
import com.vispractice.vpshop.entity.Member;
import com.vispractice.vpshop.service.CompanyService;
import com.vispractice.vpshop.service.MemberService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.vispractice.alipay.model.pay.WebPayDetail;
import com.vispractice.vpshop.entity.Company;
import com.vispractice.vpshop.entity.Order;
import com.vispractice.vpshop.entity.Order.OrderStatus;
import com.vispractice.vpshop.plugin.AlipaySupport;
import com.vispractice.vpshop.plugin.WepaySupport;
import com.vispractice.vpshop.service.OrderService;
import com.vispractice.vpshop.service.QRCodeService;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 类 编 号：UI_PU010401_PaysController
 * 类 名 称：PaysController.java
 * 内容摘要：
 * 完成日期：2016年8月23日 下午5:33:00
 * 编码作者: 彭龙
 */
@Controller("paysController")
@RequestMapping("/pays/")
public class PaysController {

    private static final Logger log = LoggerFactory.getLogger(PaysController.class);

    @Autowired
    private WepaySupport wepaySupport;
    @Autowired
    private AlipaySupport alipaySupport;
    
    @Resource(name = "orderServiceImpl")
	private OrderService orderService;
    @Autowired
	private QRCodeService qrCodeService;
    @Resource
    private AgentCompanyService agentCompanyService;
    @Resource(name = "memberServiceImpl")
    private MemberService memberService;
    @Resource(name = "companyServiceImpl")
    private CompanyService companyService;
    @Resource(name = "jedisDao")
    private JedisDao jedisDao;


    /**
     * 二维码支付
     * @param orderNumber 商户订单号
     */
    /**
     * 方法:二维码支付
     * 实现流程:
     *    1.判断订单状态是未支付且未过期
     *    2.生成支付二维码链接
     *    3.生成相应二维码返回页面
     * autor :彭龙
     * @param orderNumber
     * @param response
     */
    @RequestMapping(value = "/qrpay")
    public void qrPay(
            @RequestParam("sn") String sn,
            HttpServletResponse response){
        	Order order = orderService.findBySn(sn);
        	log.info("订单微信支付申请："+sn);
        	String qrUrl = "";
        	//当订单状态为未支付且订单支付时间没有过期
        	if(order.getOrderStatus() == OrderStatus.unpaid && !order.isExpired())
        	{
        		 qrUrl = wepaySupport.qrPay(order);
        	}
        	else{
        		qrUrl = "此订单已支付";
        	}
           
            //二维码图片输出流  
            OutputStream out = null;  
            try{  
                response.setContentType("image/jpeg;charset=UTF-8");  
                BufferedImage image = qrCodeService.createQRCode(qrUrl);  
                //实例化输出流对象  
                out = response.getOutputStream();  
                //画图  
                ImageIO.write(image, "png", response.getOutputStream());  
                out.flush();  
                out.close();  
            }catch (Exception e){  
                e.printStackTrace();  
            }finally {  
                try{  
                    if (null != response.getOutputStream()) {  
                        response.getOutputStream().close();  
                    }  
                    if (null != out) {  
                        out.close();  
                    }  
                }catch(Exception e){  
                    e.printStackTrace();  
                }  
            } 
    }
    
    /**
     * 方法:支付宝支付
     * 实现流程:
     * autor :彭龙
     * @param orderNumber
     * @param response
     */
    @RequestMapping("/aliweb")
    public void webPay(@RequestParam("sn") String sn, HttpServletRequest request, HttpServletResponse resp){
    	String form = "无支付";
    	Order order = orderService.findBySn(sn);
    	Company company=order.getMember().getCompanyId();
    	log.info("订单支付宝支付申请："+sn);
    	if(order.getOrderStatus() == OrderStatus.unpaid && !order.isExpired())
    	{
    		
    		 WebPayDetail detail = new WebPayDetail(sn, order.getOrderItems().get(0).getName(), order.getAmountPayable().setScale(2, BigDecimal.ROUND_HALF_UP).toString());
    	     form = alipaySupport.webPay(detail,company,request);
    	}
       
        try {
            resp.setContentType("text/html;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(form);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            // ignore
        }
    }


    /**
     *
     * 方法: 生成代理公司平台支付请求重定向链接，跳转到代理平台收银台<br/>
     * 实现流程  : <br/>
     * @param model
     * @param request
     * @param resp
     * @param orderSn	订单编号
     * @return
     */
    @RequestMapping(value = "/agentCompanyUri")
    public @ResponseBody
    Message agentCompanyUri(ModelMap model, HttpServletRequest request, HttpServletResponse resp, String orderSn){
        Message checkMsg = checkOrder(orderSn);//验证订单状态
        if(checkMsg != null){//验证不通过
            return checkMsg;
        }

        //支付链接生成  1为pc,2为移动端，不填默认为pc
        return agentCompanyService.paymentUri(orderSn,request.getSession(),"1");
    }

    /**
     *
     * 方法: 订单状态验证<br/>
     * 实现流程  : <br/>
     * @param orderSn	订单号
     * @return
     * 编码作者 : 吴锭超
     * 完成日期 : 2020-04-21 20:15:23
     */
    private Message checkOrder(String orderSn){
        Order order = orderService.findBySn(orderSn);
        if (order == null) {//订单不存在
            log.info("订单{}不存在", orderSn);
            return Message.error("订单不存在");
        }
        Member member = memberService.getCurrent();
        if (member == null || !member.getId().equals(order.getMember().getId())) {
            log.info("会员不存在或订单不属于此会员");
            return Message.error("订单不存在");
        }
        // 订单是否为待支付状态
        if (order.getOrderStatus() != OrderStatus.unpaid) {//订单状态不是待支付
            log.info("订单状态已不再是未支付状态，订单号：{}", orderSn);
            if(order.getOrderStatus() != OrderStatus.cancelled){//不是取消订单
                return Message.warn("已支付");
            }
            return Message.error("订单状态不可支付");
        }
        if (order.isExpired()) {//订单已过期
            log.info("订单:{}已过期", orderSn);
            return Message.error("订单状态不可支付");
        }

        //如果订单有积分支付, 判断企业是否有limitAmount授信额度限制,如果是需要判断本订单加已经消费的总额是否超过企业充值总额，超过不允许下单
        if(companyService.amountLimit(member.getCompanyId(), order, order.getSupplierId().getIsOweOrder())){
            log.info("订单:{}超过企业授信额度", orderSn);
            if(member.getCompanyId().getId() == CommonAttributes.YPH_COMPANY_ID
                    || member.getCompanyId().getId() == CommonAttributes.HENGLI_COMPANY_ID
                    || member.getCompanyId().getId() == CommonAttributes.HENGLI_HIGH_COMPANY_ID
                    || StringUtils.isNotEmpty(member.getCompanyId().getSourceFlag())){ // 代理平台
                return Message.error(Constants.YPH_LIMIT_AMOUNT_TIPS);
            }
            return Message.error(Constants.COMPANY_LIMIT_AMOUNT_TIPS);
        } else {//已充值
            String coinFirstFlag = jedisDao.STRINGS.get(Constants.ORDER_COIN_FIRST_FLAG + order.getCompanyId().getId());
            if(StringUtils.isNotBlank(coinFirstFlag)){//有限消费选择了积分的订单
                if(order.getCoinAmount() != null && order.getCoinAmount().compareTo(BigDecimal.ZERO) > 0){//选择了积分消费
                } else {//没有选择积分消费，返回很快回来
                    return Message.error(Constants.ORDER_COIN_FIRST_TIPS);
                }
            }
        }

        return null;
    }
}
