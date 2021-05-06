package net.xdclass.contorller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Tag;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.constant.CacheKey;
import net.xdclass.enums.ClientType;
import net.xdclass.enums.ProductOrderPayTypeEnum;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.model.LoginUser;
import net.xdclass.request.ConfirmOrderRequest;
import net.xdclass.request.RepayOrderRequest;
import net.xdclass.service.ProductOrderService;
import net.xdclass.utils.CommonUtil;
import net.xdclass.utils.JsonData;
import org.apiguardian.api.API;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 二当家小D
 * @since 2021-03-01
 */

@Api("订单模块")
@RestController
@RequestMapping("/api/order/v1")
@Slf4j
public class ProductOrderController {
    @ApiParam
    private ProductOrderService productOrderService;
    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation("生成订单")
    @PostMapping("ConfirmOrder")
    public void confirmOrder(@RequestBody ConfirmOrderRequest orderRequest, HttpServletResponse response) {
        JsonData jsonData = productOrderService.confirmOrder(orderRequest);
        //响应流输出数据
        if (jsonData.getCode() == 0) {
            log.info("创建订单成功{}", orderRequest.toString());
            String clientType = orderRequest.getClientType();//端类型
            log.info(clientType);
            String payType = orderRequest.getPayType();//支付类型

            if (payType.equalsIgnoreCase(ProductOrderPayTypeEnum.ALIPAY.name())) {
                if (clientType.equalsIgnoreCase(ClientType.H5.name())) {
                    //支付宝网页支付都是跳转到支付宝支付网页 APP除外
                    writeData(response, jsonData);
                }
                if (clientType.equalsIgnoreCase(ClientType.APP.name())) {
                    //支付宝APP支付
                }

            } else if (payType.equalsIgnoreCase(ProductOrderPayTypeEnum.WECHAT.name())) {
                //微信支付

            } else if (payType.equalsIgnoreCase(ProductOrderPayTypeEnum.BANK.name())) {
                //网银支付

            } else {
                //抛出异常 必须选择支付方式
            }

        } else {
            //失败
            log.info("创建订单失败 order{}", jsonData.toString());
        }
    }

    /**
     * 响应流输出方法
     *
     * @param response
     * @param jsonData
     */
    private void writeData(HttpServletResponse response, JsonData jsonData) {
        response.setContentType("text/html;charset=UTF8");
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.write(jsonData.getData().toString());
            writer.flush();
        } catch (Exception e) {
            log.error("写出支付宝HTML异常{}", e);
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }

    /**
     * RPC根据订单号查询订单状态
     *
     * @param outTradeNo
     * @return
     */
    @PostMapping("/queryOrderState")
    public JsonData queryOrderState(String outTradeNo) {
        String result = productOrderService.queryOrderState(outTradeNo);
        return JsonData.buildSuccess(result);

    }

    /**
     * 二次请求支付
     *
     * @param repayOrderRequest
     * @param response
     * @return
     */
    @ApiOperation("二次支付请求")
    @PostMapping("repay")
    public void repay(@RequestBody RepayOrderRequest repayOrderRequest, HttpServletResponse response) {
        JsonData jsonData = productOrderService.repay(repayOrderRequest);

        if (jsonData.getCode() == 0) {

            String client = repayOrderRequest.getClientType();
            String payType = repayOrderRequest.getPayType();

            //如果是支付宝网页支付，都是跳转网页，APP除外
            if (payType.equalsIgnoreCase(ProductOrderPayTypeEnum.ALIPAY.name())) {

                log.info("重新支付订单成功:{}", repayOrderRequest.toString());

                if (client.equalsIgnoreCase(ClientType.H5.name())) {
                    writeData(response, jsonData);//直接写出

                } else if (client.equalsIgnoreCase(ClientType.APP.name())) {
                    //APP SDK支付  TODO
                }

            } else if (payType.equalsIgnoreCase(ProductOrderPayTypeEnum.WECHAT.name())) {

                //微信支付 TODO
            }

        } else {
            log.error("重新支付订单失败{}", jsonData.toString());
            CommonUtil.sendJsonMsg(response, jsonData);//此处是直接把JsonData 错误响应转字符串返回给前端
        }
    }

    /**
     * 获取订单防重令牌
     * 1.下单前先调用此接口获取到防重令牌 再调用下单接口
     * @return
     */
    @ApiOperation("获取订单防重令牌")
    @GetMapping("get_token")
    public JsonData getOrderToken(){
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        //缓存键格式化
        String cacheOrderTokenKey=String.format(CacheKey.SUBMIT_ORDER_TOKEN_KEY,loginUser.getId());
        String token = CommonUtil.getStringNumRandom(32);
        redisTemplate.opsForValue().set(cacheOrderTokenKey,token,30,TimeUnit.MINUTES);
        return JsonData.buildSuccess(token);
    }
}

