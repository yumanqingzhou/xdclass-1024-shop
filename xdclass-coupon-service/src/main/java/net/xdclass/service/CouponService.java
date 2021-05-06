package net.xdclass.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.xdclass.enums.CouponCategoryEnum;
import net.xdclass.model.CouponDO;
import com.baomidou.mybatisplus.extension.service.IService;
import net.xdclass.request.NewUserCouponRequest;
import net.xdclass.utils.JsonData;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-15
 */
public interface CouponService extends IService<CouponDO> {


    Map<String,Object> pageCouponActivity(int page, int size);

    JsonData redisAddCoupon(Long couponId, CouponCategoryEnum promotion);

    JsonData addCoupon(Long couponId, CouponCategoryEnum promotion);

    JsonData initNewUserCoupon(NewUserCouponRequest couponRequest);
}
