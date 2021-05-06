package net.xdclass.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class LockProductRequest {
    @JsonProperty("order_out_trade_no")
    private String orderOutTradeNo;

    @JsonProperty("order_item_list")
    private List<OrderItemRequest> orderItemList;
}
