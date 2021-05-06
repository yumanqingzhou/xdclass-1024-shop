package net.xdclass.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class OrderItemRequest {
    @JsonProperty("product_id")
    private long productId;

    @JsonProperty("buy_num")
    private int buyNum;

}
