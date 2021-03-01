package net.xdclass.contorller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Tag;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.ClientType;
import net.xdclass.enums.ProductOrderPayTypeEnum;
import net.xdclass.request.ConfirmOrderRequest;
import net.xdclass.service.ProductOrderService;
import net.xdclass.utils.JsonData;
import org.apiguardian.api.API;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

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

            }else {
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
}

