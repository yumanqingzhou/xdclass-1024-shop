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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
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
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 分页查询所有优惠券
     * 1.新人券不返回 只返回促销券
     *
     * @param page
     * @param size
     * @return
     */
    @Override
    public Map<String, Object> pageCouponActivity(int page, int size) {
        Page<CouponDO> pageInfo = new Page(page, size);
        Page<CouponDO> couponDOPage = couponMapper.selectPage(pageInfo, new QueryWrapper<CouponDO>().eq("publish", CouponPublishEnum.PUBLISH)
                .eq("category", CouponCategoryEnum.PROMOTION).orderByDesc("create_time"));

        long total = couponDOPage.getTotal();//总条数
        long pages = couponDOPage.getPages();//总页数
        Map<String, Object> pageMap = new HashMap<>();
        pageMap.put("total_record", total);
        pageMap.put("total_page", pages);
        //封装结果集
        List<CouponVO> collect = couponDOPage.getRecords().stream().map(obj -> beanProcess(obj)).collect(Collectors.toList());
        pageMap.put("current_data", collect);
        return pageMap;
    }

    /**
     * 基于Redis实现分布式锁的领劵接口
     * 1、获取优惠券是否存在
     * 2、校验优惠券是否可以领取：时间、库存、超过限制
     * 3、扣减库存
     * 4、保存领劵记录
     * <p>
     * 始终要记得，羊毛党思维很厉害，社会工程学 应用的很厉害
     *
     * @param couponId
     * @param category
     * @return
     */
    @Override
    public JsonData redisAddCoupon(Long couponId, CouponCategoryEnum category) {
        //TODO 1.并发下超售优惠券 2.远程调用扣减库存失败
        //获取当前登录对象
        LoginUser loginUser = LoginInterceptor.threadLocal.get();


        //分布式锁 解决同一用户超领优惠券问题 锁最好用 用户ID作为锁  粒度更小 这样刚好解决同个用户超领问题 如果用优惠券ID作为锁
        //则多个用户在领相同ID优惠券时也是阻塞状态
        String lockKey = "lock:coupon:" + couponId;
        //每个线程对用一个UUID 作为值存入Redis中 防止其他线程删除了自己线程的锁
        String value = CommonUtil.getUUID();
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(lockKey, value, Duration.ofSeconds(30));
        if (flag) {
            log.info("加锁成功");
            try {
                //查询是否存在该ID优惠券
                CouponDO couponDO = couponMapper.selectOne(new QueryWrapper<CouponDO>()
                        .eq("id", couponId)
                        .eq("category", category.name()));
                //检测优惠券合法性
                checkCoupon(couponDO, loginUser.getId());
                //构建领取优惠券记录对象模型
                CouponRecordDO couponRecordDO = new CouponRecordDO();
                BeanUtils.copyProperties(couponDO, couponRecordDO);
                couponRecordDO.setCreateTime(new Date());
                couponRecordDO.setUseState(CouponStateEnum.NEW.name());
                couponRecordDO.setUserId(loginUser.getId());
                couponRecordDO.setUserName(loginUser.getName());
                couponRecordDO.setCouponId(couponId);
                couponRecordDO.setId(null);
                //扣减库存 TODO v1:存在多扣减库存的问题 通过sql已修复
                //mysql innodb 本身具有行锁 在进行update时 会暂时锁住该行数据 因此可以同个sql解决
                int rows = couponMapper.reduceStock(couponId);

                //存入领取记录表
                if (rows == 1) {
                    //库存扣减成功才保存记录
                    couponRecordMapper.insert(couponRecordDO);

                } else {
                    log.warn("发放优惠券失败:{},用户:{}", couponDO, loginUser);

                    throw new BizException(BizCodeEnum.COUPON_NO_STOCK);
                }


            } finally {
                //luna脚本释放锁
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Integer result = redisTemplate.execute(new DefaultRedisScript<>(script, Integer.class), Arrays.asList(lockKey), value);
            }
        } else {
            //加锁失败 自旋后重新尝试获取锁
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            redisAddCoupon(couponId, category);
        }


        return JsonData.buildSuccess();

    }

    /**
     * Redisson分布式锁 实现领券
     * @param couponId
     * @param category
     * @return
     */
    @Transactional(rollbackFor=Exception.class,propagation= Propagation.REQUIRED)
    @Override
    public JsonData addCoupon(Long couponId, CouponCategoryEnum category){
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        String lockKey = "lock:coupon:" + couponId;
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();//默认30秒过期 有看门狗机制 每10秒检查 当前Redisson客户端是否还持有key 如果有则自动续期
        //lock.lock(10,TimeUnit.SECONDS); 指定过期时间 无看门狗机制 到期后不管程序有没有执行完成 都释放锁
        try {
            log.info("加锁成功");
            //查询是否存在该ID优惠券
            CouponDO couponDO = couponMapper.selectOne(new QueryWrapper<CouponDO>()
                    .eq("id", couponId)
                    .eq("category", category.name()));
            //检测优惠券合法性
            checkCoupon(couponDO, loginUser.getId());
            //构建领取优惠券记录对象模型
            CouponRecordDO couponRecordDO = new CouponRecordDO();
            BeanUtils.copyProperties(couponDO, couponRecordDO);
            couponRecordDO.setCreateTime(new Date());
            couponRecordDO.setUseState(CouponStateEnum.NEW.name());
            couponRecordDO.setUserId(loginUser.getId());
            couponRecordDO.setUserName(loginUser.getName());
            couponRecordDO.setCouponId(couponId);
            couponRecordDO.setId(null);
            //扣减库存 TODO v1:存在多扣减库存的问题 通过sql已修复
            //mysql innodb 本身具有行锁 在进行update时 会暂时锁住该行数据 因此可以同个sql解决
            int rows = couponMapper.reduceStock(couponId);

            //存入领取记录表
            if (rows == 1) {
                //库存扣减成功才保存记录
                couponRecordMapper.insert(couponRecordDO);

            } else {
                log.warn("发放优惠券失败:{},用户:{}", couponDO, loginUser);

                throw new BizException(BizCodeEnum.COUPON_NO_STOCK);
            }

        }finally {
            lock.unlock();
        }
        return JsonData.buildSuccess();
    }
    /**
     * 优惠券对象DO转换VO
     *
     * @param obj
     * @return
     */
    private CouponVO beanProcess(CouponDO obj) {
        CouponVO couponVO = new CouponVO();
        BeanUtils.copyProperties(obj, couponVO);
        return couponVO;
    }

    /**
     * 检查优惠券合法性
     *
     * @param couponDO
     * @param userId
     */
    private void checkCoupon(CouponDO couponDO, Long userId) {
        //是否存在该优惠券
        if (couponDO == null) {
            throw new BizException(BizCodeEnum.COUPON_NO_EXITS);
        }
        //优惠券是否为发布状态
        if (!couponDO.getPublish().equalsIgnoreCase(CouponPublishEnum.PUBLISH.name())) {
            throw new BizException(BizCodeEnum.COUPON_GET_FAIL);
        }

        //判断是否库存足够
        if (couponDO.getStock() <= 0) {
            throw new BizException(BizCodeEnum.COUPON_NO_STOCK);
        }

        //判断时间是否有效
        long time = CommonUtil.getCurrentTimestamp();
        long start = couponDO.getStartTime().getTime();
        long end = couponDO.getEndTime().getTime();
        if (time < start || time > end) {
            throw new BizException(BizCodeEnum.COUPON_OUT_OF_TIME);
        }

        //判断是否领取数量超过限制
        QueryWrapper<CouponRecordDO> wapper = new QueryWrapper<CouponRecordDO>()
                .eq("coupon_id", couponDO.getId())
                .eq("user_id", userId);
        //领取条数 如果该用户领取的条数大于等于限制的条数就不能再领了
        //TODO 存在超领的问题 多并发下 当前领取记录还未保存到数据库领取记录表时  容易多个线程同时通过校验
        Integer recordNum = couponRecordMapper.selectCount(wapper);
        if (couponDO.getUserLimit() <= recordNum) {
            throw new BizException(BizCodeEnum.COUPON_OUT_OF_LIMIT);
        }
    }
}
