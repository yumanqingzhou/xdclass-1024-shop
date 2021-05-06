package net.xdclass.listener;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.model.CouponRecordMessage;
import net.xdclass.service.CouponRecordService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Slf4j
@RabbitListener(queues = "${mqconfig.coupon_release_queue}")
public class CouponStockMQListener {

    @Autowired
    private CouponRecordService couponRecordService;
    @Autowired
    private RedissonClient redissonClient;

    @RabbitHandler
    public void releaseCouponRecord(CouponRecordMessage recordMessage, Message message, Channel channel) throws IOException {
        log.info("监听到优惠券锁定消息：{}", recordMessage);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();//MQ为消息分配的唯一编号
        //如果怕多线程同时操作同一条消息 可以1.采用分布式锁 2.幂等性字段
        //Lock lock = redissonClient.getLock("lock:coupon_record_release:"+recordMessage.getTaskId());
        //lock.lock();
        /**
         * 重试次数判断 可以将这个消息唯一的标识 deliveryTag 存入Redis 如果重试一次 就次数+1  3次后插入
         * 故障表 人工排查
         */
        Boolean aBoolean = couponRecordService.releaseCouponRecord(recordMessage);
        try {
            if (aBoolean){
                //处理成功
                channel.basicAck(deliveryTag,true);
            }else {
                //处理失败  重复投递一下
                channel.basicReject(deliveryTag,true);
            }
        } catch (IOException e) {
            log.error("释放优惠券记录异常:{},msg:{}",e,recordMessage);
            channel.basicReject(deliveryTag,true);
        }
    }
}
