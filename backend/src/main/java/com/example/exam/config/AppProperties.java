package com.example.exam.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.upload")
public class AppProperties {

    private String dir = "./uploads";

    private String downloadPathPrefix = "/api/file/download";
}
