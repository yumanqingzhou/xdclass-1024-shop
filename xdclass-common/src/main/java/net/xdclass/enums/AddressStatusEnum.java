package net.xdclass.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AddressStatusEnum {
    /**
     * 是默认收货地址
     */
    DEFAULT_STATUS(1),

    /**
     * 非默认收货地址
     */
    COMMON_STATUS(0);
    private int code;

}
