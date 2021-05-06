package net.xdclass.mq;

import net.xdclass.CouponApplication;
import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.service.CouponRecordService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CouponApplication.class)
public class CouponMQTest {
    @Autowired
    private CouponRecordService couponRecordService;

    @Test
    public void  testCouponMQ(){
        LockCouponRecordRequest couponRecordRequest=new LockCouponRecordRequest();
        List<Long> ids=new ArrayList<>();
        ids.add(141L);
        couponRecordRequest.setLockCouponRecordIds(ids);
        couponRecordRequest.setLockCouponRecordIds(ids);
        couponRecordRequest.setOrderOutTradeNo("123456abc");
        couponRecordService.lockCouponRecords(couponRecordRequest);
    }
}
