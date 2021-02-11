package net.xdclass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "aliyun.oss")
@Configuration
@Data
public class OssConfig {
    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String bucketname;
}
