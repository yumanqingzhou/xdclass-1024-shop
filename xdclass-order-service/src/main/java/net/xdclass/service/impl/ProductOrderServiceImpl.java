package net.xdclass.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.compoment.PayFactory;
import net.xdclass.config.OrderMQConfig;
import net.xdclass.constant.CacheKey;
import net.xdclass.constant.TimeConstant;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.ProductOrderPayTypeEnum;
import net.xdclass.enums.ProductOrderStateEnum;
import net.xdclass.enums.ProductOrderTypeEnum;
import net.xdclass.exception.BizException;
import net.xdclass.feign.CouponServiceFeign;
import net.xdclass.feign.ProductServiceFeign;
import net.xdclass.feign.UserServiceFeign;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.model.LoginUser;
import net.xdclass.model.OrderMessage;
import net.xdclass.model.ProductOrderDO;
import net.xdclass.mapper.ProductOrderMapper;
import net.xdclass.request.*;
import net.xdclass.service.ProductOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.xdclass.utils.CommonUtil;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.CouponRecordVO;
import net.xdclass.vo.OrderItemVO;
import net.xdclass.vo.PayInfoVO;
import net.xdclass.vo.ProductOrderAddressVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-03-01
 */
@Service
@Slf4j
public class ProductOrderServiceImpl implements ProductOrderService {
    @Autowired
    private ProductOrderMapper productOrderMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private UserServiceFeign userServiceFeign;
    @Autowired
    private CouponServiceFeign couponServiceFeign;
    @Autowired
    private ProductServiceFeign productServiceFeign;
    @Autowired
    private OrderMQConfig orderMQConfig;
    @Autowired
    private PayFactory payFactory;
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 生成订单
     * service编写伪代码
     * 防重提交 这种只能解决 同一订单被多次支付的情况  但是不能解决相同商品生成多个订单的情况  因为这种事提交了订单+支付
     * 还有一种是 提交订单 生成预览订单 然后拿到token 支付时带上token 然后校验通过 可以支付 避免同一物品多次生成订单
     * ⽤户微服务-确认收货地址
     * 商品微服务-获取最新购物项和价格
     * 订单验价
     * 优惠券微服务-获取优惠券
     * 验证价格
     * 锁定优惠券
     * 锁定商品库存
     * 创建订单对象
     * 创建⼦订单对象
     * 发送延迟消息-⽤于⾃动关单
     * 创建⽀付信息-对接三⽅⽀付
     *
     * @param confirmOrderRequest
     * @return
     */
    @Override
    public JsonData confirmOrder(ConfirmOrderRequest confirmOrderRequest) {
        //生成订单号
        String outTradeNo = CommonUtil.getUUID();
        //获取登录用户
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        //luna脚本校验防重令牌
        //原子操作 校验令牌，删除令牌
        String orderToken = confirmOrderRequest.getToken();
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(String.format(CacheKey.SUBMIT_ORDER_TOKEN_KEY, loginUser.getId())), orderToken);
        if (result==0){
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_TOKEN_EQUAL_FAIL);
        }
        //远程查询用户收货地址
        long addressId = confirmOrderRequest.getAddressId();
        ProductOrderAddressVO userAddress = this.getUserAddress(addressId);
        //PRC获取优惠券详情
        Long couponRecordId = confirmOrderRequest.getCouponRecordId();
        CouponRecordVO couponRecord = this.getCouponRecord(couponRecordId);
        //校验价格
        List<Long> productIdList = confirmOrderRequest.getProductIdList();
        //获取选中购物项最新价格 并同时删除购物车中结算的购物项
        JsonData jsonData = productServiceFeign.confirmOrderCartItem(productIdList);
        List<OrderItemVO> orderItemVOList = jsonData.getData(new TypeReference<>() {
        });
        log.info("获取的商品:{}", orderItemVOList);
        if (orderItemVOList == null) {
            //购物车商品不存在
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_CART_ITEM_NOT_EXIST);
        }
        //验证价格，减去商品优惠券
        this.checkPrice(orderItemVOList, confirmOrderRequest);
        //RPC-锁定优惠券
        this.lockCouponRecords(confirmOrderRequest, outTradeNo);
        //RPC-锁定库存
        this.lockProductStocks(orderItemVOList, outTradeNo);
        //创建订单对象并保存
        ProductOrderDO productOrderDO = this.saveProductOrder(confirmOrderRequest, loginUser, outTradeNo, userAddress);
        //发送MQ消息
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOutTradeNo(outTradeNo);
        rabbitTemplate.convertAndSend(orderMQConfig.getEventExchange(), orderMQConfig.getOrderCloseDelayRoutingKey(), orderMessage);
        //创建支付
        PayInfoVO payInfoVO = new PayInfoVO(outTradeNo,
                productOrderDO.getPayAmount(), confirmOrderRequest.getPayType(),
                confirmOrderRequest.getClientType(), orderItemVOList.get(0).getProductTitle(), "", TimeConstant.ORDER_PAY_TIMEOUT_MILLS);
        String payResult = payFactory.pay(payInfoVO);
        if (StringUtils.isNotEmpty(payResult)) {
            log.info("创建支付订单成功:payInfoVO={},payResult={}", payInfoVO, payResult);
            return JsonData.buildSuccess(payResult);
        } else {
            log.error("创建支付订单失败:payInfoVO={},payResult={}", payInfoVO, payResult);
            return JsonData.buildResult(BizCodeEnum.PAY_ORDER_FAIL);
        }

    }

    /**
     * 根据订单号查询订单状态
     *
     * @param outTradeNo
     * @return
     */
    @Override
    public String queryOrderState(String outTradeNo) {
        if (StringUtils.isNotEmpty(outTradeNo)) {
            ProductOrderDO productOrderDO = productOrderMapper.selectOne(
                    new QueryWrapper<ProductOrderDO>().eq("out_trade_no", outTradeNo));
            if (productOrderDO == null) {
                return "";
            }
            return productOrderDO.getState();

        } else {
            throw new BizException(BizCodeEnum.PAY_ORDER_NOT_EXIST);
        }
    }


    /**
     * 关闭订单
     * 1.去数据库查询订单状态
     * 如果是已支付不用处理
     * 注意已取消也要去第三方查询一下 很可能本地系统取消了 但是支付那边已经支付了 所以要查询一下支付状态
     * 2.如果是未支付 去第三方查询支付状态 确定未支付 关闭订单
     *
     * @param orderMessage
     * @return
     */
    @Override
    public Boolean closeProductOrder(OrderMessage orderMessage) {
        //查询订单状态
        String outTradeNo = orderMessage.getOutTradeNo();
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(new QueryWrapper<ProductOrderDO>().eq("out_trade_no", outTradeNo));
        if (productOrderDO == null) {
            //订单不存在
            log.warn("直接确认消息，订单不存在:{}", orderMessage);
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_NOT_EXIST);
        }
        if (productOrderDO.getState().equalsIgnoreCase(ProductOrderStateEnum.PAY.name())) {
            //已经支付
            log.info("直接确认消息,订单已经支付:{}", orderMessage);
            return true;
        }
        //向第三方支付查询订单是否真的未支付
        PayInfoVO payInfoVO = new PayInfoVO();
        payInfoVO.setPayType(productOrderDO.getPayType());
        payInfoVO.setOutTradeNo(orderMessage.getOutTradeNo());
        String payResult = payFactory.queryPaySuccess(payInfoVO);

        //结果为空，则未支付成功，本地取消订单
        if (StringUtils.isBlank(payResult)) {
            productOrderMapper.updateOrderPayState(productOrderDO.getOutTradeNo(), ProductOrderStateEnum.CANCEL.name(), ProductOrderStateEnum.NEW.name());
            log.info("结果为空，则未支付成功，本地取消订单:{}", orderMessage);
            return true;
        } else {
            //支付成功，主动的把订单状态改成UI就支付，造成该原因的情况可能是支付通道回调有问题
            log.warn("支付成功，主动的把订单状态改成UI就支付，造成该原因的情况可能是支付通道回调有问题:{}", orderMessage);
            productOrderMapper.updateOrderPayState(productOrderDO.getOutTradeNo(), ProductOrderStateEnum.PAY.name(), ProductOrderStateEnum.NEW.name());
            return true;
        }

    }

    /**
     * 支付回调处理逻辑
     * 更新订单状态
     * 不管微信支付还是支付宝支付 业务层都是调用此方法 所以在该方法中要通过传入的枚举参数 判断是哪个第三方支付
     * 只有支付成功会触发通知 关闭和失败不会触发异步回调通知
     *
     * @param payType
     * @param paramsMap
     * @return
     */
    @Override
    public JsonData handlerOrderCallbackMsg(ProductOrderPayTypeEnum payType, Map<String, String> paramsMap) {
        //1.如果并发大 可以采用MQ投递 --》在慢慢消费

        if (payType.name().equalsIgnoreCase(ProductOrderPayTypeEnum.ALIPAY.name())) {
            //阿里支付回调
            //获取商户订单号
            String outTradeNo = paramsMap.get("out_trade_no");
            //交易的状态
            String tradeStatus = paramsMap.get("trade_status");
            if ("TRADE_SUCCESS".equalsIgnoreCase(tradeStatus) || "TRADE_FINISHED".equalsIgnoreCase(tradeStatus)) {
                //更新订单状态
                productOrderMapper.updateOrderPayState(outTradeNo, ProductOrderStateEnum.PAY.name(), ProductOrderStateEnum.NEW.name());
                return JsonData.buildSuccess();
            }
//            if ("TRADE_CLOSED".equalsIgnoreCase(tradeStatus)){
//                //未付款交易超时关闭 或交易成功后全额退款
//
//            }
        } else if (payType.name().equalsIgnoreCase(ProductOrderPayTypeEnum.WECHAT.name())) {
            //微信支付  TODO
        }
        //如果响应参数MAP里没有交易状态 则返回给回调处理controller 处理业务逻辑失败
        return JsonData.buildResult(BizCodeEnum.PAY_ORDER_CALLBACK_NOT_SUCCESS);
    }

    /**
     * 二次支付订单请求
     * 解决方案1.用户第一次发送请求的时候 就把响应存入缓存中 如果第二次再次发送请求 就从缓存中拿出来响应给用户
     * 优点 少一次和第三方支付的交互 缺点 缓存占用本地内存资源
     * 解决方案2。再次发起第三方请求支付 计算订单过期时间  重新给一个过期时间
     * 本项目采用的是方案2
     *
     * @param repayOrderRequest
     * @return
     */
    @Override
    public JsonData repay(RepayOrderRequest repayOrderRequest) {
        //获取订单号
        String outTradeNo = repayOrderRequest.getOutTradeNo();
        //获取登录用户
        Long userId = LoginInterceptor.threadLocal.get().getId();
        //查询订单详情
        ProductOrderDO productOrderDO = productOrderMapper.selectOne(new QueryWrapper<ProductOrderDO>()
                .eq("out_trade_no", outTradeNo)
                .eq("user_id", userId));
        if (productOrderDO == null) {
            return JsonData.buildResult(BizCodeEnum.PAY_ORDER_NOT_EXIST);
        }

        if (!productOrderDO.getState().equalsIgnoreCase(ProductOrderStateEnum.NEW.name())) {
            return JsonData.buildResult(BizCodeEnum.PAY_ORDER_STATE_ERROR);
        } else {
            //获取订单已存活时间  现在时间-订单创建时间
            long createTime = productOrderDO.getCreateTime().getTime();
            long nowTime = CommonUtil.getCurrentTimestamp();
            long orderLiveTime = nowTime - createTime;
            //创建订单是临界点，所以再增加1分钟多几秒，假如29分，则也不能支付了
            orderLiveTime = orderLiveTime + 70 * 1000;
            //如果存活时间大于订单超时时间 不允许支付
            if (orderLiveTime > TimeConstant.ORDER_PAY_TIMEOUT_MILLS) {
                return JsonData.buildResult(BizCodeEnum.PAY_ORDER_PAY_TIMEOUT);
            } else {
                //记得更新DB订单支付参数 payType，还可以增加订单支付信息日志  TODO
                //总时间-存活的时间 = 剩下的有效时间  此处只计算了剩余有效时间 没有判断剩余时间是否小于1分钟 如果小于1分钟了 也不允许支付
                //判断逻辑在支付宝支付策略模式中的计算模板中定义了
                long timeOut = TimeConstant.ORDER_PAY_TIMEOUT_MILLS - orderLiveTime;
                // 构建支付请求参数 并设置新过期时间
                PayInfoVO payInfoVO = new PayInfoVO(productOrderDO.getOutTradeNo(),
                        productOrderDO.getPayAmount(), repayOrderRequest.getPayType(),
                        repayOrderRequest.getClientType(), productOrderDO.getOutTradeNo(), "", timeOut);
                log.info("payInfoVO={}", payInfoVO);
                String payResult = payFactory.pay(payInfoVO);
                //判断状态 支付失败 是返回空
                if (StringUtils.isNotBlank(payResult)) {
                    log.info("创建二次支付订单成功:payInfoVO={},payResult={}", payInfoVO, payResult);
                    return JsonData.buildSuccess(payResult);
                } else {
                    log.error("创建二次支付订单失败:payInfoVO={},payResult={}", payInfoVO, payResult);
                    return JsonData.buildResult(BizCodeEnum.PAY_ORDER_FAIL);
                }

            }


            // 发起请求
        }

    }


    /**
     * 查询用户收货地址详情
     */
    public ProductOrderAddressVO getUserAddress(long addressId) {
        JsonData jsonData = userServiceFeign.detail(addressId);
        if (jsonData.getCode() != 0) {
            log.error("获取收获地址失败,msg:{}", jsonData);
            throw new BizException(BizCodeEnum.ADDRESS_NO_EXITS);
        }

        ProductOrderAddressVO addressVO = jsonData.getData(new TypeReference<ProductOrderAddressVO>() {
        });
        return addressVO;

    }

    /**
     * 获取优惠券
     *
     * @param couponRecordId
     * @return
     */
    public CouponRecordVO getCouponRecord(Long couponRecordId) {
        if (couponRecordId == null || couponRecordId < 0) {
            return null;
        }
        JsonData jsonData = couponServiceFeign.getCoupondetail(couponRecordId);
        if (jsonData.getCode() != 0) {
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_COUPON_FAIL);
        }
        if (jsonData.getCode() == 0) {

            CouponRecordVO couponRecordVO = jsonData.getData(new TypeReference<>() {
            });

            if (!couponAvailable(couponRecordVO)) {
                log.error("优惠券使用失败");
                throw new BizException(BizCodeEnum.COUPON_UNAVAILABLE);
            }
            return couponRecordVO;
        }
        return null;

    }

    /**
     * 判断优惠券是否可用
     * 1.时间是否符合
     *
     * @param couponRecordVO
     * @return
     */
    private boolean couponAvailable(CouponRecordVO couponRecordVO) {
        long start = couponRecordVO.getStartTime().getTime();
        long end = couponRecordVO.getEndTime().getTime();
        long nowTime = CommonUtil.getCurrentTimestamp();
        if (nowTime >= start && nowTime <= end) {
            return true;
        }
        return false;
    }

    /**
     * 验价
     * 1.获取前端传来的价格
     * 2.获取所有商品总价
     * 3.获取优惠券抵用价格
     * 4，总价-优惠券=实际支付价格
     * 5.和前端传来的价格比对
     *
     * @param orderItemVOS
     * @param confirmOrderRequest
     */
    public void checkPrice(List<OrderItemVO> orderItemVOS, ConfirmOrderRequest confirmOrderRequest) {
        BigDecimal prevRealPayAmount = confirmOrderRequest.getRealPayAmount();
        //总价
        BigDecimal sumPrice = new BigDecimal("0");
        for (OrderItemVO orderItemVO : orderItemVOS) {
            BigDecimal totalAmount = orderItemVO.getTotalAmount();
            sumPrice = sumPrice.add(totalAmount);
        }
        //减去优惠券使用价格
        CouponRecordVO couponRecord = this.getCouponRecord(confirmOrderRequest.getCouponRecordId());
        BigDecimal conditionPrice = couponRecord.getConditionPrice();
        if (couponRecord != null) {
            //比较总价是否符合优惠券能使用的价格 如果<0 则证明不够使用条件
            if (sumPrice.compareTo(conditionPrice) < 0) {
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_COUPON_FAIL);
            }
            //如果优惠券价格抵扣价格 大于总价
            if (couponRecord.getPrice().compareTo(sumPrice) > 0) {
                sumPrice = BigDecimal.ZERO;//总支付价格就为0
            } else {
                sumPrice = sumPrice.subtract(couponRecord.getPrice());
            }

        }
        //和前端价格验价
        if (sumPrice.compareTo(prevRealPayAmount) != 0) {
            log.error("订单验价失败：{}", confirmOrderRequest);
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_PRICE_FAIL);
        }

    }

    /**
     * 锁定优惠券
     *
     * @param orderRequest
     * @param orderOutTradeNo
     */
    public void lockCouponRecords(ConfirmOrderRequest orderRequest, String orderOutTradeNo) {
        Long couponRecordId = orderRequest.getCouponRecordId();
        //接口那边是批量锁定所以要传一个集合过去
        List<Long> couponRecordIds = new ArrayList<>();
        if (couponRecordId != null) {
            couponRecordIds.add(couponRecordId);
            //PRC 远程调用传输对象
            LockCouponRecordRequest couponRecordRequest = new LockCouponRecordRequest();
            couponRecordRequest.setOrderOutTradeNo(orderOutTradeNo);
            couponRecordRequest.setLockCouponRecordIds(couponRecordIds);
            //发起锁定优惠券请求
            JsonData jsonData = couponServiceFeign.lockCouponRecords(couponRecordRequest);
            if (jsonData.getCode() != 0) {
                log.info("订单远程调用锁优惠券服务失败", orderRequest);
                throw new BizException(BizCodeEnum.COUPON_RECORD_LOCK_FAIL);
            }
        }

    }

    /**
     * 锁定每项商品库存
     *
     * @param orderItemVOS
     * @param orderOutTradeNo
     */
    public void lockProductStocks(List<OrderItemVO> orderItemVOS, String orderOutTradeNo) {
        if (orderItemVOS != null) {
            List<OrderItemRequest> orderItemRequestList = orderItemVOS.stream().map(obj -> {
                OrderItemRequest orderItemRequest = new OrderItemRequest();
                orderItemRequest.setProductId(obj.getProductId());
                orderItemRequest.setBuyNum(obj.getBuyNum());
                return orderItemRequest;
            }).collect(Collectors.toList());

            LockProductRequest lockProductRequest = new LockProductRequest();
            lockProductRequest.setOrderItemList(orderItemRequestList);
            lockProductRequest.setOrderOutTradeNo(orderOutTradeNo);
            JsonData jsonData = productServiceFeign.lockProducts(lockProductRequest);
            if (jsonData.getCode() != 0) {
                log.error("锁定商品库存失败：{}", lockProductRequest);
                throw new BizException(BizCodeEnum.ORDER_CONFIRM_LOCK_PRODUCT_FAIL);
            }
        } else {
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_ADD_STOCK_TASK_FAIL);
        }
    }

    /**
     * 保存订单
     *
     * @param orderRequest
     * @param loginUser
     * @param orderOutTradeNo
     * @param addressVO
     * @return
     */
    public ProductOrderDO saveProductOrder(ConfirmOrderRequest orderRequest, LoginUser loginUser, String orderOutTradeNo, ProductOrderAddressVO addressVO) {

        ProductOrderDO productOrderDO = new ProductOrderDO();
        productOrderDO.setUserId(loginUser.getId());
        productOrderDO.setHeadImg(loginUser.getHeadImg());
        productOrderDO.setNickname(loginUser.getName());

        productOrderDO.setOutTradeNo(orderOutTradeNo);
        productOrderDO.setCreateTime(new Date());
        productOrderDO.setDel(0);
        productOrderDO.setOrderType(ProductOrderTypeEnum.DAILY.name());

        //实际支付的价格
        productOrderDO.setPayAmount(orderRequest.getRealPayAmount());

        //总价，未使用优惠券的价格
        productOrderDO.setTotalAmount(orderRequest.getTotalAmount());
        productOrderDO.setState(ProductOrderStateEnum.NEW.name());
        //设置什么类型的订单
        ProductOrderTypeEnum.valueOf(orderRequest.getPayType()).name();
        //设置订单支付方式
        productOrderDO.setPayType(ProductOrderPayTypeEnum.valueOf(orderRequest.getPayType()).name());
        //设置收货地址
        productOrderDO.setReceiverAddress(JSON.toJSONString(addressVO));

        productOrderMapper.insert(productOrderDO);

        return productOrderDO;

    }
}
