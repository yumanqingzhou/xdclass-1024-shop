package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.CouponCategoryEnum;
import net.xdclass.enums.CouponPublishEnum;
import net.xdclass.enums.CouponStateEnum;
import net.xdclass.exception.BizException;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.CouponRecordMapper;
import net.xdclass.model.CouponDO;
import net.xdclass.mapper.CouponMapper;
import net.xdclass.model.CouponRecordDO;
import net.xdclass.model.LoginUser;
import net.xdclass.service.CouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.xdclass.utils.CommonUtil;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.CouponVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-15
 */
@Slf4j
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, CouponDO> implements CouponService {
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private CouponRecordMapper couponRecordMapper;

    /**
     * 分页查询所有优惠券
     * 1.新人券不返回 只返回促销券
     * @param page
     * @param size
     * @return
     */
    @Override
    public Map<String,Object> pageCouponActivity(int page, int size) {
        Page<CouponDO> pageInfo=new Page(page,size);
        Page<CouponDO> couponDOPage = couponMapper.selectPage(pageInfo, new QueryWrapper<CouponDO>().eq("publish", CouponPublishEnum.PUBLISH)
                .eq("category", CouponCategoryEnum.PROMOTION).orderByDesc("create_time"));

        long total = couponDOPage.getTotal();//总条数
        long pages = couponDOPage.getPages();//总页数
        Map<String,Object> pageMap=new HashMap<>();
        pageMap.put("total_record",total);
        pageMap.put("total_page",pages);
        //封装结果集
        List<CouponVO> collect =couponDOPage.getRecords().stream().map(obj -> beanProcess(obj)).collect(Collectors.toList());
        pageMap.put("current_data",collect);
        return pageMap;
    }

    /**
     * 领劵接口
     * 1、获取优惠券是否存在
     * 2、校验优惠券是否可以领取：时间、库存、超过限制
     * 3、扣减库存
     * 4、保存领劵记录
     *
     * 始终要记得，羊毛党思维很厉害，社会工程学 应用的很厉害
     *
     * @param couponId
     * @param category
     * @return
     */
    @Override
    public JsonData addCoupon(Long couponId, CouponCategoryEnum category) {
        //TODO 1.并发下超售优惠券 2.远程调用扣减库存失败
        //获取当前登录对象
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        //查询是否存在该ID优惠券
        CouponDO couponDO = couponMapper.selectOne(new QueryWrapper<CouponDO>()
                .eq("id", couponId)
                .eq("category", category.name()));
        //检测优惠券合法性
        checkCoupon(couponDO,loginUser.getId());
        //构建领取优惠券记录对象模型
        CouponRecordDO couponRecordDO = new CouponRecordDO();
        BeanUtils.copyProperties(couponDO,couponRecordDO);
        couponRecordDO.setCreateTime(new Date());
        couponRecordDO.setUseState(CouponStateEnum.NEW.name());
        couponRecordDO.setUserId(loginUser.getId());
        couponRecordDO.setUserName(loginUser.getName());
        couponRecordDO.setCouponId(couponId);
        couponRecordDO.setId(null);
        //扣减库存 TODO
        int rows=couponMapper.reduceStock(couponId);
        //存入领取记录表
        if(rows==1){
            //库存扣减成功才保存记录
            couponRecordMapper.insert(couponRecordDO);

        }else {
            log.warn("发放优惠券失败:{},用户:{}",couponDO,loginUser);

            throw  new BizException(BizCodeEnum.COUPON_NO_STOCK);
        }

        return JsonData.buildSuccess();

    }

    /**
     * 优惠券对象DO转换VO
     * @param obj
     * @return
     */
    private CouponVO beanProcess(CouponDO obj) {
        CouponVO couponVO=new CouponVO();
        BeanUtils.copyProperties(obj,couponVO);
        return couponVO;
    }

    /**
     * 检查优惠券合法性
     * @param couponDO
     * @param userId
     */
    private void  checkCoupon(CouponDO couponDO,Long userId){
        //是否存在该优惠券
        if (couponDO == null) {
            throw  new BizException(BizCodeEnum.COUPON_NO_EXITS);
        }
        //优惠券是否为发布状态
        if (!couponDO.getPublish().equalsIgnoreCase(CouponPublishEnum.PUBLISH.name())){
            throw new BizException(BizCodeEnum.COUPON_GET_FAIL);
        }

        //判断是否库存足够
        if (couponDO.getStock()<=0){
            throw new BizException(BizCodeEnum.COUPON_NO_STOCK);
        }

        //判断时间是否有效
        long time = CommonUtil.getCurrentTimestamp();
        long start = couponDO.getStartTime().getTime();
        long end = couponDO.getEndTime().getTime();
        if (time<start||time>end){
            throw new BizException(BizCodeEnum.COUPON_OUT_OF_TIME);
        }

        //判断是否领取数量超过限制
        QueryWrapper<CouponRecordDO> wapper = new QueryWrapper<CouponRecordDO>()
                .eq("coupon_id", couponDO.getId())
                .eq("user_id", userId);
        //领取条数 如果该用户领取的条数大于等于限制的条数就不能再领了
        Integer recordNum = couponRecordMapper.selectCount(wapper);
        if (couponDO.getUserLimit()<=recordNum){
            throw new BizException(BizCodeEnum.COUPON_OUT_OF_LIMIT);
        }
    }
}
