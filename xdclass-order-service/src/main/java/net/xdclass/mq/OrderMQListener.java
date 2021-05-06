package net.xdclass.mq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.model.OrderMessage;
import net.xdclass.service.ProductOrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RabbitListener(queues = "${mqconfig.order_release_queue}")
public class OrderMQListener {
    @Autowired
    private ProductOrderService productOrderService;
    
    @RabbitHandler
    public void checkOrder(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        log.info("监听到消息：closeProductOrder:{}",orderMessage);
        //调用检查订单是否支付成功的方法
        Boolean flag=productOrderService.closeProductOrder(orderMessage);
        try {
            if(flag){
                channel.basicAck(tag,false);
            }else {
                channel.basicReject(tag,true);
            }
        } catch (IOException e) {
            log.error("定时关单失败:",orderMessage);
            channel.basicReject(tag,true);
        }
    }
}
