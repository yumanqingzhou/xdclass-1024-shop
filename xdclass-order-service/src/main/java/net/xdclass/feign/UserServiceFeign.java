package net.xdclass.feign;

import io.swagger.annotations.ApiParam;
import net.xdclass.utils.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("xdclass-user-service")
public interface UserServiceFeign {

     @GetMapping("/api/address/v1/find/{address_id}")
     JsonData detail(@PathVariable("address_id") Long addressId);

}
