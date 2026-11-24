package com.wzu.aispringboot.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.wzu.aispringboot.common.ResultCode;
import com.wzu.aispringboot.enumclass.UserType;
import com.wzu.aispringboot.exception.BusinessException;
import com.wzu.aispringboot.util.JwtTokenUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前登录用户上下文。
 * <p>
 * token 解析集中在此，避免每个 Controller 重复 "取 token → verify → 取 claim" 三段式，
 * 也避免各处对 null 的处理不一致（有的返回 200 空数据，有的抛异常）。
 */
@Component
public class CurrentUserHolder {

    /** 取当前用户 ID，未登录直接抛业务异常 */
    public Long requireUserId() {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ResultCode.ACCESS_UNAUTHORIZED.getCode(),
                    ResultCode.ACCESS_UNAUTHORIZED.getMsg(), null);
        }
        return userId;
    }

    /** 取当前用户 ID，未登录返回 null */
    public Long currentUserIdOrNull() {
        try {
            String token = JwtTokenUtil.getCurrentToken();
            DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
            return jwt.getClaim("userId").asLong();
        } catch (Exception e) {
            return null;
        }
    }

    /** 当前用户是否为管理员 */
    public boolean isAdmin() {
        try {
            String token = JwtTokenUtil.getCurrentToken();
            DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
            Integer roleType = jwt.getClaim("roleType").asInt();
            return UserType.ADMIN.getCode().equals(roleType);
        } catch (Exception e) {
            return false;
        }
    }

    /** 当前请求的客户端 IP，用于日志与审计 */
    public String currentIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        var request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
