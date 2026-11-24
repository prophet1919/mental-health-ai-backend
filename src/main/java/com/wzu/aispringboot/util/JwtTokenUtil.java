package com.wzu.aispringboot.util;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.wzu.aispringboot.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import java.util.Date;

import static com.baomidou.mybatisplus.spring.spi.SpringCompatibleSet.applicationContext;

@Component
public class JwtTokenUtil implements ApplicationContextAware {
    private static final String ISSUER = "mental-health-assistant";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String LEGACY_TOKEN_HEADER = "token";
    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenUtil.class);
    private static ApplicationContext applicationContext;
    //用于在静态工具类中使用spring容器中的Bean
    @Override
    public void setApplicationContext(ApplicationContext applicationContext){
        JwtTokenUtil.applicationContext = applicationContext;
    }

    private static JwtConfig getJwtConfig(){
        return applicationContext.getBean(JwtConfig.class);
    }

    //生成token的方法
    public static String generateToken(Long userId,String username,Integer roleType){
        try {
            //获取JwtConfig
            JwtConfig jwtConfig = getJwtConfig();
            //生成签名的算法
            Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
            //设置过期时间
            Date expiration = new Date(System.currentTimeMillis() + jwtConfig.getExpiration());
            //创建JWT
            String token = JWT.create()
                    .withClaim("userId",userId)
                    .withClaim("username",username)
                    .withClaim("roleType",roleType)
                    .withExpiresAt(expiration)   //过期时间
                    .withIssuedAt(new Date())   //签发时间
                    .withIssuer(ISSUER)   //签发者
                    .sign(algorithm); //签名
            return token;
        }catch (Exception e){
            throw new RuntimeException("生成token失败" + e);
        }
    }

    //提取token：统一 Authorization: Bearer <token>，兼容旧的 token 头一个版本
    public static String extractTokenFromRequest(HttpServletRequest request){
        if(request == null){
            return null;
        }

        // 1) 标准方式：Authorization: Bearer <token>
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if(StringUtils.hasText(authHeader)){
            if(authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())){
                String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                if(StringUtils.hasText(token)){
                    return token;
                }
            }else{
                // 兼容不带头缀直接放 token 的调用方
                return authHeader.trim();
            }
        }

        // 2) 过渡兼容：旧的 token 头（下一版本移除）
        String legacyToken = request.getHeader(LEGACY_TOKEN_HEADER);
        if(StringUtils.hasText(legacyToken)){
            LOG.warn("检测到已废弃的 token 请求头，请改用 {}: {}<token>", AUTHORIZATION_HEADER, BEARER_PREFIX);
            return legacyToken;
        }
        return null;
    }

    //获取当前的token
    public static String getCurrentToken(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null){
            HttpServletRequest request = attributes.getRequest();
            String token = (String) request.getAttribute("jwtToken");
            if(token != null){
                return token;
            }

            //备用方案:从请求头中提取token
            String headerToken = extractTokenFromRequest(request);
            return headerToken;
        }
        return null;
    }

    //验证token
    public static TokenVerificationResult validateToken(String token){
        DecodedJWT jwt = verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        String username = jwt.getClaim("username").asString();

        //角色类型
        Integer roleType = null;
        try {
            roleType = jwt.getClaim("roleType").asInt();
        }catch (Exception e){
            String roleTypeStr = jwt.getClaim("roleType").asString();
            if(StringUtils.hasText(roleTypeStr)){
                roleType = Integer.parseInt(roleTypeStr);
            }
        }
        if(userId != null && StringUtils.hasText(username) && roleType != null){
            return new TokenVerificationResult(userId,username,roleType,true);
        }

        return null;
    }

    //验证token的有效性
    public static DecodedJWT verifyToken(String token){
        if(!StringUtils.hasText(token)){
            throw new JWTVerificationException("token不能为空");
        }
        //token解码
        JwtConfig jwtConfig = getJwtConfig();
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token);
    }

    //Token验证结果封装类
    @Getter
    public static class TokenVerificationResult{
        private final Long userId;
        private final String username;
        private final Integer roleType;
        private final boolean valid;

        public TokenVerificationResult(Long userId,String username,Integer roleType,boolean valid){
            this.userId = userId;
            this.username = username;
            this.roleType = roleType;
            this.valid = valid;
        }
    }



}
