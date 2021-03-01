package net.xdclass.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车
 */
public class CartVO {
    //购物项
    private List<CartItemVO> cartItems;
    //购物车总价
    private BigDecimal totalAmount;
    //购物车总件数
    private Integer totalNum;
    //优惠后实际支付价格
    private BigDecimal realPayAmount;

    public List<CartItemVO> getCartItems() {
        return cartItems;
    }

    /**
     * 获取购物车总价
     * 每个商品的数量乘以单价 累加
     *
     * @return
     */
    public BigDecimal getTotalAmount() {
        BigDecimal totalAmount = new BigDecimal("0");
        //此处使用foreach因为BigDecimal每次运算都会产生一个新对象 lambada表达式要求流式表达式中变量只能被赋值一次
        if (this.getCartItems() != null) {
            for (CartItemVO cartItem : this.getCartItems()) {
                BigDecimal itemTotalAmount = cartItem.getTotalAmount();
                totalAmount.add(itemTotalAmount);
            }
        }
        return totalAmount;
    }

    /**
     * 获取购物车总件数
     *
     * @return
     */
    public Integer getTotalNum() {
        if (this.getCartItems() != null) {
           int totalNum = this.getCartItems().stream().mapToInt(CartItemVO::getBuyNum).sum();
           return totalNum;
        }
        return 0;
    }

    /**
     * 获取实际支付价格
     * TODO 1.前端获取用户使用的优惠券 进行价格计算
     *      2.把优惠券ID 和 抵扣后价格传回后端 后端验价
     *
     *
     * @return
     */
    public BigDecimal getRealPayAmount() {
        BigDecimal amount = new BigDecimal("0");
        if(this.cartItems!=null){
            for(CartItemVO cartItemVO : cartItems){
                BigDecimal itemTotalAmount =  cartItemVO.getTotalAmount();
                amount = amount.add(itemTotalAmount);
            }
        }
        return amount;
    }

    public void setCartItems(List<CartItemVO> cartItems) {
        this.cartItems = cartItems;
    }
}
