package net.xdclass.compoment;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.AliPayConfig;
import net.xdclass.config.PayURLConfig;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.ClientType;
import net.xdclass.exception.BizException;
import net.xdclass.vo.PayInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * 对支付接口的具体实现
 */
@Service
@Slf4j
public class AlipayStrategy implements PayStrategy{
    @Autowired
    private PayURLConfig payURLConfig;

    /**
     * 发起支付调用的
     * 1.调用成功form 为HTML代码
     * 2.支付失败 form是“”
     * @param payInfoVO
     * @return
     */
    @Override
    public String unifiedorder(PayInfoVO payInfoVO) {
        //调用支付宝支付所需参数集合
        HashMap<String,String> content = new HashMap<>();
        //商户订单号,64个字符以内、可包含字母、数字、下划线；需保证在商户端不重复
        content.put("out_trade_no", payInfoVO.getOutTradeNo());
        content.put("product_code", "FAST_INSTANT_TRADE_PAY");
        //订单总金额，单位为元，精确到小数点后两位
        content.put("total_amount", payInfoVO.getPayFee().toString());
        //商品标题/交易标题/订单标题/订单关键字等。 注意：不可使用特殊字符，如 /，=，&amp; 等。
        content.put("subject", payInfoVO.getTitle());
        //商品描述，可空
        content.put("body", payInfoVO.getDescription());
        //TODO 判断订单是否快要到期 如果小于1分钟 不允许发起二次支付
        //这里主要是针对于二次支付的 二次支付在业务层只是判断了 是否已经超过超时支付时间 但是没有判断是否小于1分钟  此处要判断是否支付
        //时间已经小于1分钟 如果是第一次支付 基本误差不会超过2秒 所以不存在订单马上超时的情况
        System.out.println(payInfoVO.getOrderPayTimeoutMills());
        //向下四舍五入
        double timeout = Math.floor(payInfoVO.getOrderPayTimeoutMills()/(60*1000));
        if (timeout<1){
            throw  new BizException(BizCodeEnum.PAY_ORDER_PAY_TIMEOUT);
        }
        // 该笔订单允许的最晚付款时间，逾期将关闭交易。取值范围：1m～15d。m-分钟，h-小时，d-天，1c-当天（1c-当天的情况下，无论交易何时创建，都在0点关闭）。 该参数数值不接受小数点， 如 1.5h，可转换为 90m。
        content.put("timeout_express", Double.valueOf(timeout).intValue()+"m");
        //获取支付要用的SDK类型 比如H5 APP
        String clientType = payInfoVO.getClientType();
        String form="";//发起支付后 支付宝会响应给我们一个HTML页面 用户是跳转到支付宝那个HTML页面进行支付的 所以把这个页面返回给前端就可以
        //判断客户端类型
        try {
            if (clientType.equalsIgnoreCase(ClientType.H5.name())){
                //H5手机网页支付
                AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
                request.setBizContent(JSON.toJSONString(content));
                request.setNotifyUrl(payURLConfig.getAlipayCallbackUrl());//设置通知回调URL
                request.setReturnUrl(payURLConfig.getAlipaySuccessReturnUrl());//支付成功要跳转的页面
                //调用单例模式中获取支付宝支付的客户端 发起支付请求
                AlipayTradeWapPayResponse alipayResponse  = AliPayConfig.getInstance().pageExecute(request);
                if(alipayResponse.isSuccess()){
                    //获取支付响应体
                    form = alipayResponse.getBody();
                } else {
                    log.error("支付宝构建H5表单失败:alipayResponse={},payInfo={}",alipayResponse,payInfoVO);
                }


            }else if(clientType.equalsIgnoreCase(ClientType.PC.name())){
                //PC支付
                AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
                request.setBizContent(JSON.toJSONString(content));
                request.setNotifyUrl(payURLConfig.getAlipayCallbackUrl());
                request.setReturnUrl(payURLConfig.getAlipaySuccessReturnUrl());
                AlipayTradePagePayResponse alipayResponse  = AliPayConfig.getInstance().pageExecute(request);

                log.info("响应日志:alipayResponse={}",alipayResponse);

                if(alipayResponse.isSuccess()){
                    form = alipayResponse.getBody();
                } else {
                    log.error("支付宝构建PC表单失败:alipayResponse={},payInfo={}",alipayResponse,payInfoVO);
                }

            }
        } catch (AlipayApiException e) {
            log.error("支付宝构建表单异常:payInfo={},异常={}",payInfoVO,e);
            e.printStackTrace();
        }


        return form;
    }

    /**
     * 退款
     * @param payInfoVO
     * @return
     */
    @Override
    public String refund(PayInfoVO payInfoVO) {
        return null;
    }

    /**
     * 查询订单状态
     * 支付成功 返回非空
     * 其他返回空
     * @param payInfoVO
     * @return
     */
    @Override
    public String queryPaySuccess(PayInfoVO payInfoVO) {

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        HashMap<String,String > content = new HashMap<>();

        //订单商户号,64位
        content.put("out_trade_no",payInfoVO.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(content));

        AlipayTradeQueryResponse response = null;
        try {
            response = AliPayConfig.getInstance().execute(request);
            log.info("支付宝订单查询响应：{}",response.getBody());

        } catch (AlipayApiException e) {
            log.error("支付宝订单查询异常:{}",e);
        }

        if(response.isSuccess()){
            log.info("支付宝订单状态查询成功:{}",payInfoVO);
            return response.getTradeStatus();
        }else {
            log.info("支付宝订单状态查询失败:{}",payInfoVO);
            return "";
        }
    }
}
