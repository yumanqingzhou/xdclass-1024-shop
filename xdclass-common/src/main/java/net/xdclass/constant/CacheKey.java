package net.xdclass.constant;

/**
 * 小滴课堂,愿景：让技术不再难学
 *
 * @Description
 * @Author 二当家小D
 * @Remark 有问题直接联系我，源码-笔记-技术交流群
 * @Version 1.0
 **/

public class CacheKey {

    /**
     * 注册验证码，第一个是类型，第二个是接收邮箱
     */
    public static final String CHECK_CODE_KEY = "code:%s:%s";

    /**
     * 购物车缓存 哈希结构key 用户ID为唯一标识
     */
    public static final String CART_KEY="cart:%s";


    /**
     * 提交表单的token key
     */
    public static final String SUBMIT_ORDER_TOKEN_KEY = "order:submit:%s";
}
