package net.xdclass.feign;

import io.swagger.annotations.ApiParam;
import net.xdclass.request.LockProductRequest;
import net.xdclass.utils.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("xdclass-product-service")
public interface ProductServiceFeign {
    @PostMapping("/api/cart/v1/confirmOrderCartItem")
    public JsonData confirmOrderCartItem(@RequestBody List<Long> productIds);

    @PostMapping("/api/product/v1/lock_products")
    public JsonData lockProducts(@ApiParam("锁定商品对象集合")@RequestBody LockProductRequest productRequest);
}
