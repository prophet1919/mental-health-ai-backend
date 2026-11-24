package com.wzu.aispringboot.util;

import cn.hutool.json.JSONUtil;
import com.wzu.aispringboot.DTO.response.UserLoginResponseDTO;
import com.wzu.aispringboot.common.ResultCode;
import com.wzu.aispringboot.config.SecurityConfig;
import com.wzu.aispringboot.enumclass.UserStatus;
import com.wzu.aispringboot.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthticationFilter.class);

    @Resource
    private UserService userService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        // 如果是公共路径，直接放行
        return SecurityConfig.isPublicPath(requestUri);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        // 1. 从请求头中获取JWT token（Authorization: Bearer <token>）
        String token = JwtTokenUtil.extractTokenFromRequest(request);
        if(StringUtils.hasText(token)){
            // 验证token并获取用户信息
            JwtTokenUtil.TokenVerificationResult validationResult = JwtTokenUtil.validateToken(token);
            if (validationResult != null && validationResult.isValid()) {
                //3.查询用户信息，验证用户状态
                UserLoginResponseDTO.UserDetailResponseDTO user = userService.getUserById(validationResult.getUserId());
                if(user != null && UserStatus.NORMAL.getCode().equals(user.getStatus())){
                    //4.创建Spring Security认证对象
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + validationResult.getRoleType())
                    );
                    //5.创建UsernamePasswordAuthenticationToken对象
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            validationResult.getUsername(),
                            null,
                            authorities
                    );
                    //6.设置认证信息到Spring Security上下文
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    //7.将token存储到请求属性中
                    request.setAttribute("jwtToken", token);
                    request.setAttribute("userId", validationResult.getUserId());
                    request.setAttribute("roleType", validationResult.getRoleType());

                    LOG.debug("鉴权通过 uri={} method={} userId={}", requestUri, method, validationResult.getUserId());
                }else{
                    //清理上下文
                    clearSecurityContext();
                    //响应错误信息
                    ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
                    return;
                }
            }else {
                //清理上下文
                clearSecurityContext();
                //2. 响应错误信息
                ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
                return;
            }
        }else{
            //清理上下文
            clearSecurityContext();
            //2. 响应错误信息
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }
        // 继续过滤器链
        chain.doFilter(request, response);
    }

    //清理Spring Security上下文
    private void clearSecurityContext(){
        SecurityContextHolder.clearContext();
    }


}
