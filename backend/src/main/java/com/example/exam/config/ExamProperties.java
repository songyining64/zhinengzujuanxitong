package com.example.exam.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "exam")
public class ExamProperties {

    private long autoSubmitIntervalMs = 60_000L;
}
