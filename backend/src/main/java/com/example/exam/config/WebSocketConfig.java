package com.example.exam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单内存消息代理，处理以"/topic"开头的消息
        config.enableSimpleBroker("/topic", "/queue");
        // 设置应用程序目的地前缀
        config.setApplicationDestinationPrefixes("/app");
        // 设置用户目的地前缀（用于点对点消息）
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，指定使用SockJS
        registry.addEndpoint("/ws-exam")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // 纯WebSocket端点（不使用SockJS）
        registry.addEndpoint("/ws-exam")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 可以添加自定义的通道拦截器，用于身份验证和授权
        // registration.interceptors(new AuthChannelInterceptor());
    }
}