package net.xdclass.compoment;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.ProductOrderPayTypeEnum;
import net.xdclass.vo.PayInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 支付类的工厂
 * 1.根据支付类型 创建对应类型的策略上下文
 * 2.上下文调用该对应类型的支付实现类的支付方法
 *
 */
@Component
@Slf4j
public class PayFactory {
    //此处不能用多态 两个类实现了同一接口 spring 会报类型注入错误
    @Autowired
    private AlipayStrategy alipayStrategy;
    @Autowired
    private WechatPayStrategy wechatPayStrategy;

    /**
     * 创建支付总工厂
     * @param payInfoVO
     * @return
     */
    public String pay(PayInfoVO payInfoVO){
        String payType = payInfoVO.getPayType();
        //如果是阿里支付
        if (ProductOrderPayTypeEnum.ALIPAY.name().equalsIgnoreCase(payType)){
            //创建阿里支付策略上下文 并调用上下文中 执行的方法 具体的网页 还是APP支付等 在支付类中会细分 此处只判定大类别
            PayStrategyContext payStrategyContext=new PayStrategyContext(alipayStrategy);
            return payStrategyContext.executeUnifiedorder(payInfoVO);
        }else if (ProductOrderPayTypeEnum.WECHAT.name().equalsIgnoreCase(payType)){
            //创建微信支付上下文
            PayStrategyContext payStrategyContext=new PayStrategyContext(wechatPayStrategy);
            return payStrategyContext.executeUnifiedorder(payInfoVO);
        }
        return "";
    }

    /**
     * 查询订单支付状态
     *
     * 支付成功返回非空，其他返回空
     *
     * @param payInfoVO
     * @return
     */
    public String queryPaySuccess(PayInfoVO payInfoVO){
        String payType = payInfoVO.getPayType();

        if(ProductOrderPayTypeEnum.ALIPAY.name().equalsIgnoreCase(payType)){
            //支付宝支付
            PayStrategyContext payStrategyContext = new PayStrategyContext(alipayStrategy);

            return payStrategyContext.executeQueryPaySuccess(payInfoVO);

        } else if(ProductOrderPayTypeEnum.WECHAT.name().equalsIgnoreCase(payType)){
            //微信支付 暂未实现
            PayStrategyContext payStrategyContext = new PayStrategyContext(wechatPayStrategy);

            return payStrategyContext.executeQueryPaySuccess(payInfoVO);
        }
        return "";

    }
}
