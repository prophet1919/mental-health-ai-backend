package com.wzu.aispringboot.util;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.wzu.aispringboot.config.JwtConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.baomidou.mybatisplus.spring.spi.SpringCompatibleSet.applicationContext;

@Component
public class JwtTokenUtil implements ApplicationContextAware {
    private static final String ISSUER = "mental-health-assistant";
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
}
