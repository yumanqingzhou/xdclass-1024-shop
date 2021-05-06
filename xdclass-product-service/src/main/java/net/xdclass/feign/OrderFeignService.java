package net.xdclass.feign;

import net.xdclass.utils.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient("xdclass-order-service")
public interface OrderFeignService {

    @PostMapping("/api/order/v1/queryOrderState")
    JsonData queryProductOrderState(String outTradeNo);
}
