package net.xdclass.fegin;

import net.xdclass.request.NewUserCouponRequest;
import net.xdclass.utils.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("xdclass-coupon-service")
public interface CouponFeginService {
        /**
         * 新用户注册发放优惠券
         * @param newUserCouponRequest
         * @return
         */
        @PostMapping("/api/coupon/v1/new_user_coupon")
        JsonData addNewUserCoupon(@RequestBody NewUserCouponRequest newUserCouponRequest);

}
