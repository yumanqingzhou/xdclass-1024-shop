package net.xdclass.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.ProductOrderStateEnum;
import net.xdclass.enums.StockTaskStateEnum;
import net.xdclass.exception.BizException;
import net.xdclass.feign.OrderFeignService;
import net.xdclass.mapper.ProductMapper;
import net.xdclass.model.ProductDO;
import net.xdclass.model.ProductMessage;
import net.xdclass.model.ProductTaskDO;
import net.xdclass.mapper.ProductTaskMapper;
import net.xdclass.request.LockProductRequest;
import net.xdclass.request.OrderItemRequest;
import net.xdclass.service.ProductService;
import net.xdclass.service.ProductTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.ProductVO;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Order;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jyt
 * @since 2021-04-07
 */
@Service
@Slf4j
public class ProductTaskServiceImpl implements ProductTaskService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductTaskMapper productTaskMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderFeignService orderFeignService;

    /**
     * 锁定商品库存
     * 1.获取商品项信息
     * 2.获取订单号
     * 3.增加商品表锁定库存量  修改商品的锁定库存量 总库存-锁定库存>=购买数
     * 4.插入锁定task表 商品项的锁定记录
     * 5.发送锁定商品的信息
     *
     * @param productRequest
     * @return
     */
    @Transactional
    @Override
    public JsonData lockProductStock(LockProductRequest productRequest) {
        if (productRequest == null) {
            throw new BizException(BizCodeEnum.ORDER_CONFIRM_CART_ITEM_NOT_EXIST);
        }
        String orderOutTradeNo = productRequest.getOrderOutTradeNo();
        if (StringUtils.isNotEmpty(orderOutTradeNo)) {
            List<OrderItemRequest> orderItemList = productRequest.getOrderItemList();
            if (orderItemList != null && orderItemList.size() > 0) {
                //提取所有订单项ID
                List<Long> productIds = orderItemList.stream().map(OrderItemRequest::getProductId).collect(Collectors.toList());
                //批量查询出所有商品项详情
                List<ProductVO> productVOS = productService.findProductByIdBatch(productIds);
                //把每个商品根据ID分组 转为map 自己的ID为键 自己整个为值
                Map<Long, ProductVO> collect = productVOS.stream().collect(Collectors.toMap(ProductVO::getId, Function.identity()));
                for (OrderItemRequest orderItemRequest : orderItemList) {
                    //更新每个商品项锁定库存
                    int updateRows = productMapper.lockProductStock(orderItemRequest.getProductId(), orderItemRequest.getBuyNum());
                    if (updateRows != 1) {
                        throw new BizException(BizCodeEnum.ORDER_CONFIRM_LOCK_PRODUCT_FAIL);
                    } else {
                        //构建锁定记录表数据
                        ProductTaskDO productTaskDO = new ProductTaskDO();
                        productTaskDO.setProductId(orderItemRequest.getProductId());
                        productTaskDO.setBuyNum(orderItemRequest.getBuyNum());
                        productTaskDO.setProductName(collect.get(orderItemRequest.getProductId()).getTitle());
                        productTaskDO.setOutTradeNo(orderOutTradeNo);
                        productTaskDO.setLockState(StockTaskStateEnum.LOCK.name());
                        productTaskDO.setCreateTime(new Date());
                        int insert = productTaskMapper.insert(productTaskDO);
                        //发送消息
                        ProductMessage productMessage = new ProductMessage();
                        productMessage.setOutTradeNo(orderOutTradeNo);
                        productMessage.setTaskId(productTaskDO.getId());
                        rabbitTemplate.convertAndSend(productMessage);
                        log.info("商品库存锁定信息延迟消息发送成功:{}", productMessage);
                        return JsonData.buildSuccess();
                    }
                }
            } else {
                return JsonData.buildError("参数错误,无购买的商品项");
            }
        } else {
            return JsonData.buildError("参数错误,订单号为空");
        }


        return null;
    }

    /**
     * 释放解锁库存
     * 1.收到消息 获取订单号 查询记录表状态  如果是锁定状态就处理
     * 2.查询该订单的状态 如果是new 则丢回重新 如果是完结 则修改锁定表为完结
     * 如果订单不存在或者取消则释放商品库存
     * 3.taskID获取到商品ID 锁定的库存量 去商品表把锁定的库存量-掉
     *
     * @param productMessage
     * @return
     */
    @Transactional
    @Override
    public Boolean releaseProductStock(ProductMessage productMessage) {
        Long taskId = productMessage.getTaskId();
        if (taskId == null) {
            throw new BizException(BizCodeEnum.PAY_ORDER_STATE_ERROR);
        }
        //查询锁定表
        ProductTaskDO taskDO = productTaskMapper.selectById(taskId);
        if (taskDO == null) {
            throw new BizException(BizCodeEnum.PAY_ORDER_STATE_ERROR);
        }
        if (taskDO.getLockState().equalsIgnoreCase(StockTaskStateEnum.LOCK.name())) {
            //锁定状态处理
            String outTradeNo = taskDO.getOutTradeNo();
            JsonData jsonData = orderFeignService.queryProductOrderState(outTradeNo);
            if (jsonData.getCode() == 0) {
                String orderStatus = jsonData.getData().toString();
                if (ProductOrderStateEnum.NEW.name().equalsIgnoreCase(orderStatus)) {
                    //新建状态重新投递
                    log.warn("订单状态是NEW,返回给消息队列，重新投递:{}", productMessage);
                    return false;
                }
                //已支付状态 更细task表
                if (ProductOrderStateEnum.PAY.name().equalsIgnoreCase(orderStatus)) {
                    //更新task表
                    taskDO.setLockState(StockTaskStateEnum.FINISH.name());
                    int updateRows = productTaskMapper.updateById(taskDO);
                    log.info("订单已经支付，修改库存锁定工作单FINISH状态:{}", productMessage);
                    return true;

                }
                //订单状态是取消或者不存在 更新task表 并释放锁定库存
                Long productId = taskDO.getProductId();
                Integer buyNum = taskDO.getBuyNum();
                log.warn("订单不存在，或者订单被取消，确认消息,修改task状态为CANCEL,恢复商品库存,message:{}", productMessage);
                productMapper.unlockProductStock(productId, buyNum);
                taskDO.setLockState(StockTaskStateEnum.CANCEL.name());
                productTaskMapper.updateById(taskDO);
                return true;

            } else {
                log.info("远程查询订单状态失败,订单编号:{}", taskDO.getOutTradeNo());
                return false;
            }

        } else {
            log.info("商品锁定表状态不是LOCK:{}", taskDO);
            return true;
        }
    }
}
