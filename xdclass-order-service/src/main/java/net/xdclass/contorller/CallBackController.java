package net.xdclass.contorller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.AliPayConfig;
import net.xdclass.enums.ProductOrderPayTypeEnum;
import net.xdclass.service.ProductOrderService;
import net.xdclass.utils.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Api("支付回调通知模块")
@Controller
@RequestMapping("/api/callback/order/v1")
@Slf4j
public class CallBackController {
    @Autowired
    private ProductOrderService productOrderService;

    /**
     * 支付回调通知 post方式
     *
     * @param request
     * @param response
     * @return
     */
    @PostMapping("alipay")
    public String alipayCallback(HttpServletRequest request, HttpServletResponse response) {
        //将异步通知中收到的所有参数存储到map中
        Map<String, String> paramsMap = convertRequestParamsToMap(request);
        log.info("支付宝回调通知结果:{}", paramsMap);
        //调用SDK验证签名 所有回调过来的数据都需要用支付宝给我的公钥验证签名
        try {
            //验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, AliPayConfig.ALIPAY_PUB_KEY, AliPayConfig.CHARSET, AliPayConfig.SIGN_TYPE);
            if (signVerified) {
                JsonData jsonData = productOrderService.handlerOrderCallbackMsg(ProductOrderPayTypeEnum.ALIPAY, paramsMap);
                if (jsonData.getCode() == 0) {
                    //通知结果确认成功，不然会一直通知，八次都没返回success就认为交易失败
                    return "success";
                }
            }
        } catch (AlipayApiException e) {
            log.info("支付宝回调验证签名失败:异常：{}，参数:{}", e, paramsMap);
            e.printStackTrace();
        }
        //支付宝固定的失败返回字符串
        return "failure";
    }

    private Map<String, String> convertRequestParamsToMap(HttpServletRequest request) {
        //转成entry set对象 方便 遍历MAP -》此处复习遍历map的四种方式
        Set<Map.Entry<String, String[]>> entries = request.getParameterMap().entrySet();
        Map<String, String> parmsMap = new HashMap<>(16);
        for (Map.Entry<String, String[]> entry : entries) {
            String key = entry.getKey();
            String[] value = entry.getValue();
            int size = value.length;
            if (size == 1) {
                parmsMap.put(key, value[0]);
            } else {
                parmsMap.put(key, "");
            }
        }
        return parmsMap;
    }
}
