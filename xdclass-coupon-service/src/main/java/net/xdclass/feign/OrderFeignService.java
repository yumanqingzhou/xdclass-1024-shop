package net.xdclass.feign;

import net.xdclass.utils.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import springfox.documentation.spring.web.json.Json;

@FeignClient("xdclass-order-service")
public interface OrderFeignService {

    @RequestMapping("/api/order/v1/queryOrderState")
    JsonData queryProductOrderState(String outTradeNo);
}
