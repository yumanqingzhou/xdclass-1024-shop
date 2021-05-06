package net.xdclass.feign;

import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.utils.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("xdclass-coupon-service")
public interface CouponServiceFeign {

     @GetMapping("/api/coupon_record/v1detail/{record_id}")
     JsonData getCoupondetail(@PathVariable("record_id")Long recordId);

     @PostMapping("lock_records")
     public JsonData lockCouponRecords(@RequestBody LockCouponRecordRequest recordRequest);
}
