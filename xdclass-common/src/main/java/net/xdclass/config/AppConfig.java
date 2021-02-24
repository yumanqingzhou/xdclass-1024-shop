package net.xdclass.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     * @return
     */
    @Bean
    public RedissonClient redissonClient(){
        Config config=new Config();
        //单节点模式
        config.useSingleServer().setPassword(redisPwd).setAddress("redis://"+redisHost+":"+redisPort);
        //集群模式
        //config.useClusterServers().addNodeAddress("redis://10.0.29.30:6379", "redis://10.0.29.95:6379");

        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
