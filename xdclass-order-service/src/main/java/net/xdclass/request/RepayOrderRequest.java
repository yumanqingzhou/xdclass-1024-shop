package net.xdclass.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 小滴课堂,愿景：让技术不再难学
 *
 * @Description
 * @Author 二当家小D
 * @Remark 有问题直接联系我，源码-笔记-技术交流群
 * @Version 1.0
 **/

@Data
public class RepayOrderRequest {


    /**
     * 订单号
     */
    @JsonProperty("out_trade_no")
    private String outTradeNo;



    /**
     * 支付类型- 微信-银行卡-支付宝
     */
    @JsonProperty("pay_type")
    private String payType;


    /**
     * 客户端类型
     */
    @JsonProperty("client_type")
    private String clientType;
}
