package net.xdclass.mq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.model.ProductMessage;
import net.xdclass.service.ProductTaskService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RabbitListener(queues = "${mqconfig.stock_release_queue}")
@Component
public class ProductMqListener {
    @Autowired
    private ProductTaskService productTaskService;

    @RabbitHandler
    public void  releaseProductStock(ProductMessage productMessage, Message message, Channel channel) throws IOException {
        long msgTag = message.getMessageProperties().getDeliveryTag();//获取MQ消息唯一ID
        Boolean flag=productTaskService.releaseProductStock(productMessage);
        try {
            if (flag){
                channel.basicAck(msgTag,true);
                log.error("释放商品库存成功 flag=true,{}",productMessage);
            }else {
                channel.basicReject(msgTag,true);
                log.error("释放商品库存失败 flag=false,{}",productMessage);
            }
        } catch (IOException e) {
            log.error("释放商品库存异常:{},msg:{}",e,productMessage);
            channel.basicReject(msgTag,true);
        }
    }
}
