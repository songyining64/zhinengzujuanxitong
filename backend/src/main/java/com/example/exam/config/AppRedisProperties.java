package com.example.exam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.redis")
public class AppRedisProperties {

    private String host = "localhost";

    private int port = 6379;

    private String password;

    private int database = 0;
}
