package com.wzu.aispringboot.config;

import cn.hutool.core.text.AntPathMatcher;
import com.wzu.aispringboot.util.JwtAuthticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration //标注这是一个配置类,用于配置Spring Security的SecurityContext
@EnableWebSecurity //开启Web安全配置
@EnableMethodSecurity //开启方法安全配置
public class SecurityConfig {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    /**
     * 白名单：仅限无需身份即可访问的入口。
     * 注意：/api/psychological-chat/** 不在其中——对话必须鉴权，
     * 归属校验由 Service 层完成，不依赖"控制器内再校验一次 token"。
     */
    private static final String[] PUBLIC_PATHS = {
            "/",
            "/error",
            "/api/health",
            "/api/test",
            "/api/user/login",
            "/api/user/add",
            // 公开知识文章（只读）
            "/api/knowledge/articles",
            "/api/knowledge/articles/*"
    };

    public static Boolean isPublicPath(String requestUri){
        for (String publicPath : PUBLIC_PATHS){
            if(antPathMatcher.match(publicPath, requestUri)){
                return true;
            }
        }
        return false;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthticationFilter jwtAuthticationFilter,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {

        http
                // 启用 CORS（使用 CorsConfig 中定义的规则）
                .cors((cors) -> cors.configurationSource(corsConfigurationSource))
                // 禁用CSRF保护
                .csrf((csrf) -> csrf.disable())
                // 配置会话管理为无状态会话（JWT基于Token认证）
                .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权规则
                .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
                        //公开的路径，无需登录即可访问
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // 管理员接口需 ADMIN 角色
                        .requestMatchers("/api/admin/**").hasRole("2")
                        // 其他请求需要登录认证
                        .anyRequest().authenticated()
                )
                // 配置JWT认证过滤
                .addFilterBefore(jwtAuthticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
