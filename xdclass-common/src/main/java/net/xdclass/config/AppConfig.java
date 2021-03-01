package net.xdclass.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 公共配置类
 * Redisson
 */
@Configuration
@Data
public class AppConfig {
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private String redisPort;

    @Value("${spring.redis.password}")
    private String redisPwd;

    /**
     * 配置分布式锁Redisson
     *
     * @return
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        //单节点模式
        config.useSingleServer().setPassword(redisPwd).setAddress("redis://" + redisHost + ":" + redisPort);
        //集群模式
        //config.useClusterServers().addNodeAddress("redis://10.0.29.30:6379", "redis://10.0.29.95:6379");

        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

    /**
     * redis 序列化配置 避免key value乱码 但是哈希结构还是乱码 因为没有设置
     *
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        RedisSerializer redisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(redisSerializer);
        redisTemplate.setValueSerializer(redisSerializer);
        return redisTemplate;
    }
}
