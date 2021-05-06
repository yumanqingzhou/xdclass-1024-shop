package net.xdclass.compoment;

import net.xdclass.vo.PayInfoVO;

/**
 * 定义支付具有的通用方法
 *
 */
public interface PayStrategy {
    /**
     * 下单  调用第三方支付进行支付
     * @return
     */
    String unifiedorder(PayInfoVO payInfoVO);

    /**
     *  退款
     * @param payInfoVO
     * @return
     */
    default String refund(PayInfoVO payInfoVO){return "";}


    /**
     * 查询支付是否成功
     * @param payInfoVO
     * @return
     */
    default String queryPaySuccess(PayInfoVO payInfoVO){return "";}

}
