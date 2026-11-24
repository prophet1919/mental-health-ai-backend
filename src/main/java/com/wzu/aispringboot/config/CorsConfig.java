package com.wzu.aispringboot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置。
 * <p>
 * 前端 Vite 跑在 5173，后端在 1236，端口不同即跨域。
 * 若不显式放行，浏览器会在预检阶段就拒绝请求——表现为"前端完全连不上后端"，
 * 而后端日志里连一条请求记录都不会出现。
 * <p>
 * 允许的来源通过 app.cors.allowed-origins 配置，默认覆盖本地开发常用端口。
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 精确列出开发来源；生产环境请替换为实际域名
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 允许携带自定义头（Authorization 必需）
        config.setAllowedHeaders(List.of("*"));
        // 让前端能读到这些响应头
        config.setExposedHeaders(List.of("Content-Disposition"));
        // 允许携带 cookie（当前用 Bearer，保留以便后续切换）
        config.setAllowCredentials(true);
        // 预检结果缓存 1 小时，减少 OPTIONS 次数
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
