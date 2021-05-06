package net.xdclass.mapper;

import net.xdclass.model.CouponTaskDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.xdclass.request.LockCouponRecordRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author jyt
 * @since 2021-04-07
 */
public interface CouponTaskMapper extends BaseMapper<CouponTaskDO> {
    //批量插入锁定激励
    int insertBatch(@Param("couponTaskDOS") List<CouponTaskDO> couponTaskDOS);
}
