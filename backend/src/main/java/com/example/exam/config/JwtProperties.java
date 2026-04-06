package com.example.exam.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Raw secret; HS256 key is derived via SHA-256 digest in {@link com.example.exam.security.JwtTokenUtil}.
     */
    private String secret = "change-me-in-production";

    private long expirationMs = 7 * 24 * 60 * 60 * 1000L;
}
