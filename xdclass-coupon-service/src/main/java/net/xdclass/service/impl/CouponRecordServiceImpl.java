package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.MQConfig;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.CouponStateEnum;
import net.xdclass.enums.ProductOrderStateEnum;
import net.xdclass.enums.StockTaskStateEnum;
import net.xdclass.exception.BizException;
import net.xdclass.feign.OrderFeignService;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.CouponTaskMapper;
import net.xdclass.model.CouponRecordDO;
import net.xdclass.mapper.CouponRecordMapper;
import net.xdclass.model.CouponRecordMessage;
import net.xdclass.model.CouponTaskDO;
import net.xdclass.model.LoginUser;
import net.xdclass.request.LockCouponRecordRequest;
import net.xdclass.service.CouponRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.xdclass.service.CouponTaskService;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.CouponRecordVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-15
 */
@Service
@Slf4j
public class CouponRecordServiceImpl  implements CouponRecordService {

    @Autowired
    private CouponRecordMapper couponRecordMapper;
    @Autowired
    private CouponTaskMapper couponTaskMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MQConfig mqConfig;
    @Autowired
    private OrderFeignService orderFeignSerivce;

    /**
     * 分页查询已领优惠券列表
     *
     * @param page
     * @param size
     * @return
     */
    @Override
    public Map<String, Object> page(Integer page, Integer size) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        //封装分页信息
        Page<CouponRecordDO> pageInfo = new Page<>(page, size);
        Page<CouponRecordDO> myPage = couponRecordMapper.selectPage(pageInfo, new QueryWrapper<CouponRecordDO>()
                .eq("user_id", loginUser.getId())
                .orderByDesc("create_time"));
        Map<String, Object> pageMap = new HashMap<>();
        pageMap.put("total_record", myPage.getTotal());
        pageMap.put("total_page", myPage.getPages());
        pageMap.put("current_data", myPage.getRecords().stream().map(obj -> beanProcess(obj)).collect(Collectors.toList()));
        return pageMap;
    }

    /**
     * 根据领取记录ID 查询已领取优惠券详情
     *
     * @param recordId
     * @return
     */
    @Override
    public CouponRecordVO findById(Long recordId) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();//判断是否当前用户 防止水平越权攻击
        QueryWrapper<CouponRecordDO> wrapper = new QueryWrapper<>();
        wrapper.eq("id", recordId).eq("user_id", loginUser.getId());
        CouponRecordDO couponRecordDO = couponRecordMapper.selectOne(wrapper);
        CouponRecordVO couponRecordVO = this.beanProcess(couponRecordDO);
        if (couponRecordVO == null) {
            return null;
        }
        return couponRecordVO;
    }

    /**
     * 锁定优惠券 并插入优惠券锁定task表
     *
     * @param recordRequest
     * @return
     */
    @Transactional
    @Override
    public JsonData lockCouponRecords(LockCouponRecordRequest recordRequest) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        String orderOutTradeNo = recordRequest.getOrderOutTradeNo();//订单号
        //批量更新用户领取表优惠券状态状态
        int updateRows = couponRecordMapper.updateLockUseStateBatch(loginUser.getId(), CouponStateEnum.USED.name(), recordRequest.getLockCouponRecordIds());
        //插入锁定记录task表
        List<CouponTaskDO> couponTaskDOS = recordRequest.getLockCouponRecordIds().stream().map(id -> {
            CouponTaskDO couponTaskDO = new CouponTaskDO();
            couponTaskDO.setCouponRecordId(id);
            couponTaskDO.setLockState(StockTaskStateEnum.LOCK.name());
            couponTaskDO.setCreateTime(new Date());
            couponTaskDO.setOutTradeNo(orderOutTradeNo);
            return couponTaskDO;
        }).collect(Collectors.toList());
        int inserRows = couponTaskMapper.insertBatch(couponTaskDOS);
        if (recordRequest.getLockCouponRecordIds().size() == updateRows && updateRows == inserRows) {
            for (CouponTaskDO couponTaskDO : couponTaskDOS) {
                //构建MQ消息对象
                CouponRecordMessage recordMessage = new CouponRecordMessage();
                recordMessage.setTaskId(couponTaskDO.getId());
                recordMessage.setOutTradeNo(couponTaskDO.getOutTradeNo());
                //发送
                rabbitTemplate.convertAndSend(mqConfig.getEventExchange(), mqConfig.getCouponReleaseDelayRoutingKey(), recordMessage);//直接发送消息不会阻塞
                log.info("优惠券锁定消息发送成功:{}", recordMessage.toString());
            }
            return JsonData.buildSuccess();
        } else {
            throw new BizException(BizCodeEnum.COUPON_RECORD_LOCK_FAIL);
        }

    }

    /**
     * 解锁优惠券
     *
     * @param recordMessage
     * @return
     */
    @Transactional
    public Boolean releaseCouponRecord(CouponRecordMessage recordMessage) {
        Long taskId = recordMessage.getTaskId();
        //查询一下订单状态
        CouponTaskDO couponTaskDO = couponTaskMapper.selectById(taskId);
        //
        if (couponTaskDO != null) {
            String outTradeNo = couponTaskDO.getOutTradeNo();
            //锁状态才需要处理
            if (couponTaskDO.getLockState().equalsIgnoreCase(StockTaskStateEnum.LOCK.name())) {
                //查询订单状态
                JsonData jsonData = orderFeignSerivce.queryProductOrderState(recordMessage.getOutTradeNo());
                if (jsonData.getCode() == 0) {
                    String orderStatus = (String) jsonData.getData();
                    //如果订单时新建状态则丢回给队列重新处理
                    if (ProductOrderStateEnum.NEW.name().equalsIgnoreCase(orderStatus)) {
                        //状态是NEW新建状态，则返回给消息队，列重新投递
                        log.warn("订单状态是NEW,返回给消息队列，重新投递:{}", recordMessage);
                        return false;//返回false 让监听那边拒绝
                    } else if (ProductOrderStateEnum.PAY.name().equalsIgnoreCase(orderStatus)) {
                        //已经支付 修改锁定表状态为finish
                        couponTaskDO.setLockState(StockTaskStateEnum.FINISH.name());
                        int i = couponTaskMapper.updateById(couponTaskDO);
                        if (i == 1) {
                            log.info("订单已经支付，修改库存锁定工作单FINISH状态:{}", recordMessage);
                            return true;
                        } else {
                            log.info("订单已经支付，修改库存锁定工作单FINISH状态失败:{}", recordMessage);
                            return false;
                        }
                    }

                }
                //订单取消或者不存在  释放锁定记录 修改优惠券状态
                log.info("订单取消或不存在:{}", recordMessage);
                couponTaskDO.setLockState(StockTaskStateEnum.CANCEL.name());
                //更新锁定记录
                couponTaskMapper.updateById(couponTaskDO);
                //更新领券记录
                couponRecordMapper.updateStatus(couponTaskDO.getCouponRecordId(), CouponStateEnum.NEW.name());
                return true;
            } else {
                log.warn("工作单状态不是LOCK,state={},消息体={}", couponTaskDO.getLockState(), recordMessage);
                return true;
            }
        } else {
            log.warn("工作单不存，消息:{}", recordMessage);
            return true;
        }
    }

    private CouponRecordVO beanProcess(CouponRecordDO couponRecordDO) {
        CouponRecordVO couponRecordVO = new CouponRecordVO();
        BeanUtils.copyProperties(couponRecordDO, couponRecordVO);
        return couponRecordVO;
    }


}
