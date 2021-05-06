package net.xdclass.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.service.CouponRecordService;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.CouponRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-15
 */

@RestController
@RequestMapping("/api/coupon_record/v1")
public class CouponRecordController {

    @Autowired
    private CouponRecordService couponRecordService;

    /**
     * 分页查询已领优惠券
     * @return
     */
    @ApiOperation("分页查询已领优惠券")
    @GetMapping("page")
    public JsonData page(@RequestParam(value = "page",defaultValue = "1")Integer page,
                         @RequestParam(value = "size",defaultValue = "6") Integer size){
        Map<String,Object> pageResult=couponRecordService.page(page,size);
        return JsonData.buildSuccess(pageResult);
    }

    @ApiOperation("查询已领优惠券详情")
    @GetMapping("detail/{record_id}")
    public JsonData getCoupondetail(@PathVariable("record_id")Long recordId){

         CouponRecordVO couponRecordVO=couponRecordService.findById(recordId);
        return couponRecordVO==null?JsonData.buildResult(BizCodeEnum.COUPON_NO_EXITS):JsonData.buildSuccess(couponRecordVO);
    }

    @ApiOperation("RPC-锁定优惠券")
    @PostMapping("lock_records")
    public JsonData lockCouponRecords(@RequestBody LockCouponRecordRequest recordRequest){
       JsonData result= couponRecordService.lockCouponRecords(recordRequest);
       return result;
    }
}

