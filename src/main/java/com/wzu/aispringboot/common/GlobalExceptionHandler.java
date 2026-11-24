package com.wzu.aispringboot.common;

import com.wzu.aispringboot.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.stream.Collectors;


@RestControllerAdvice// 全局异常处理类,用于处理所有控制器抛出的异常
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 从异常中获取校验失败的字段和错误信息
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(","));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(), errorMsg);
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        LOG.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        //如果异常携带数据,则返回数据,否则返回默认错误信息
        if (e.getData() != null) {
            return Result.error(e.getCode(), e.getMessage(), e.getData());
        }
        return Result.error(e.getCode(), e.getMessage(), null);
    }

    // 请求体缺失或 JSON 格式错误
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.error(ResultCode.PARAM_INVALID.getCode(), "请求体格式错误", null);
    }

    // 缺少必填查询参数
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<String> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(ResultCode.PARAM_MISSING.getCode(),
                "缺少必要参数：" + e.getParameterName(), null);
    }

    /**
     * 异步/SSE 请求已不可用：客户端主动断开、或异步超时。
     * <p>
     * 这是 SSE 长连接的正常现象，不是服务端故障。
     * 必须单独处理：此时响应已 committed，无法再写任何 JSON，
     * 若落到兜底分支会触发 "Cannot render error page ... response has already been committed"。
     * 返回值设为 void，让 Spring 直接结束，不再尝试渲染错误页。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncUnusable(AsyncRequestNotUsableException e,
                                    HttpServletResponse response) {
        LOG.debug("SSE 连接已关闭（客户端断开或超时），忽略：{}", e.getMessage());
        // 响应已提交，不能再写；此处保持静默
    }

    // 兜底：未预期异常不把堆栈抛给前端
    @ExceptionHandler(Exception.class)
    public Result<String> handleUnexpected(Exception e,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        // 关键防御：SSE 流式响应一旦开始写，就不能再返回 JSON 错误体。
        // 否则 Spring 会尝试渲染 /error 页面并打印 "response has already been committed"。
        if (response.isCommitted()) {
            LOG.debug("响应已提交（通常为 SSE 流），无法返回错误体：{}", e.getMessage());
            return null;
        }
        LOG.error("未预期异常 uri={}", request.getRequestURI(), e);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "服务内部错误，请稍后重试", null);
    }
}
