package com.wzu.aispringboot.util;



import cn.hutool.json.JSONUtil;
import com.wzu.aispringboot.common.Result;
import com.wzu.aispringboot.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class ResponseUtil {
    //过滤器中的异常响应
    public static void writeError(HttpServletResponse response, ResultCode resultCode){
        //根据不同的状态码返回不同的响应体
        int status = switch (resultCode){
            case UNAUTHORIZED, ACCESS_UNAUTHORIZED, TOKEN_INVALID, TOKEN_EXPIRED, TOKEN_BLOCKED -> HttpStatus.UNAUTHORIZED.value();
            case TOKEN_ACCESS_FORBIDDEN -> HttpStatus.FORBIDDEN.value();
            default -> HttpStatus.BAD_REQUEST.value();
        };
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter writer = response.getWriter()){
            String jsonResponse = JSONUtil.toJsonStr(Result.error(resultCode.getCode(), resultCode.getMsg(), null));
            writer.write(jsonResponse);
            writer.flush();//确保响应体立即发送到输出流
        }catch (IOException e){
            System.out.println("响应异常" + e.getMessage());
        }
    }
}
