package net.xdclass.service;

import net.xdclass.model.ProductOrderDO;
import com.baomidou.mybatisplus.extension.service.IService;
import net.xdclass.request.ConfirmOrderRequest;
import net.xdclass.utils.JsonData;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-03-01
 */
public interface ProductOrderService {

    JsonData confirmOrder(ConfirmOrderRequest confirmOrderRequest);
}
