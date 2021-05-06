package net.xdclass.compoment;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.vo.PayInfoVO;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 策略模式的上下文对象
 * 屏蔽高层模块对策略、算法的直接访问，封装可能存在的变化
 * 即 对阿里支付 或微信支付进行封装 需要用什么支付方式即传入什么支付方式的对象
 */

@Slf4j
public class PayStrategyContext {

    private PayStrategy payStrategy;

    public PayStrategyContext(PayStrategy payStrategy){
        this.payStrategy=payStrategy;
    }

    /**
     * 根据支付策略调用不同的支付
     * payStrategy 在创建上下文对象的时候就已经传入 所以此处只有根据上层传入的
     * payStrategy 直接调用支付方法即可
     */
    public String executeUnifiedorder(PayInfoVO payInfoVO){
       return payStrategy.unifiedorder(payInfoVO);
    }

    /**
     * 根据支付的策略，调用不同的查询订单支持状态
     * @param payInfoVO
     * @return
     */
    public String executeQueryPaySuccess(PayInfoVO payInfoVO){
       return payStrategy.queryPaySuccess(payInfoVO);
    }

}
