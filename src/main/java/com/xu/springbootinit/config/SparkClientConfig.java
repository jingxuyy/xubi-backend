package com.xu.springbootinit.config;

import io.github.briqt.spark4j.SparkClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: xuJing
 * @date: 2024/4/10 16:53
 */

@Configuration
@ConfigurationProperties(prefix = "spring.ai")
@Data
public class SparkClientConfig {

    private String appid;
    private String apiKey;
    private String apiSecret;

    @Bean
    public SparkClient sparkClient(){
        SparkClient sparkClient = new SparkClient();
        sparkClient.appid = appid;
        sparkClient.apiKey = apiKey;
        sparkClient.apiSecret = apiSecret;
        return sparkClient;
    }
}
