package net.xdclass.service;

import net.xdclass.enums.ProductOrderPayTypeEnum;
import net.xdclass.model.OrderMessage;
import net.xdclass.model.ProductOrderDO;
import com.baomidou.mybatisplus.extension.service.IService;
import net.xdclass.request.ConfirmOrderRequest;
import net.xdclass.request.RepayOrderRequest;
import net.xdclass.utils.JsonData;

import java.util.Map;

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

    String queryOrderState(String outTradeNo);


    Boolean closeProductOrder(OrderMessage orderMessage);

    JsonData handlerOrderCallbackMsg(ProductOrderPayTypeEnum alipay, Map<String, String> paramsMap);

    JsonData repay(RepayOrderRequest repayOrderRequest);

}
