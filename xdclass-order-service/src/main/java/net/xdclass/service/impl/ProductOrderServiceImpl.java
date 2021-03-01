package net.xdclass.service.impl;

import net.xdclass.model.ProductOrderDO;
import net.xdclass.mapper.ProductOrderMapper;
import net.xdclass.request.ConfirmOrderRequest;
import net.xdclass.service.ProductOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.xdclass.utils.JsonData;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-03-01
 */
@Service
public class ProductOrderServiceImpl extends ServiceImpl<ProductOrderMapper, ProductOrderDO> implements ProductOrderService {

    /**
     * 生成订单
     * service编写伪代码
     * 防重提交
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
     * @param confirmOrderRequest
     * @return
     */
    @Override
    public JsonData confirmOrder(ConfirmOrderRequest confirmOrderRequest) {
        return null;
    }
}
