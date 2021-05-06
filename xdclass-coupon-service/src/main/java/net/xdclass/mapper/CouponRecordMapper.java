package net.xdclass.mapper;

import net.xdclass.model.CouponRecordDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-15
 */
public interface CouponRecordMapper extends BaseMapper<CouponRecordDO> {

    int updateLockUseStateBatch(@Param("userID") Long id, @Param("useState") String useState, @Param("lockCouponRecordIds") List<Long> lockCouponRecordIds);

    void updateStatus(@Param("couponRecordId") Long couponRecordId, @Param("name") String name);
}
