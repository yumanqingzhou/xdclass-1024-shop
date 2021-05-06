package net.xdclass.service;

import net.xdclass.model.CouponRecordDO;
import com.baomidou.mybatisplus.extension.service.IService;
import net.xdclass.model.CouponRecordMessage;
import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.CouponRecordVO;

import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-15
 */
public interface CouponRecordService {
    Map<String, Object> page(Integer page, Integer size);

    CouponRecordVO findById(Long recordId);

    JsonData lockCouponRecords(LockCouponRecordRequest recordRequest);

    Boolean releaseCouponRecord(CouponRecordMessage recordMessage);
}
