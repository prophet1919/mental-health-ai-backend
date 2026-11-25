package com.wzu.aispringboot.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 启动期配置校验。
 * <p>
 * 目的：避免用空密钥 / 空 API Key 静默启动。
 * 空 JWT_SECRET 会让任何人都能伪造 token；空 API Key 会让对话全部失败，
 * 而失败原因是运行时才暴露的，排查成本高。这里让问题在启动时就暴露。
 * <p>
 * 只对 JWT_SECRET 做强制校验（安全问题，且无合理默认值）；
 * DEEPSEEK_API_KEY 为空时仅告警，方便只调试非 AI 功能的场景。
 */
@Component
public class StartupConfigValidator {

    private static final Logger LOG = LoggerFactory.getLogger(StartupConfigValidator.class);

    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET 未配置。请在 application-local.yml 或环境变量中设置 jwt.secret，"
                            + "长度至少 " + MIN_SECRET_LENGTH + " 位。");
        }
        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET 长度不足（当前 " + jwtSecret.length()
                            + " 位，要求至少 " + MIN_SECRET_LENGTH + " 位）。");
        }

        if (!StringUtils.hasText(apiKey)) {
            LOG.warn("DEEPSEEK_API_KEY 未配置，AI 对话功能将不可用；登录、历史查询等接口不受影响。");
        } else if (apiKey.length() < 20) {
            LOG.warn("DEEPSEEK_API_KEY 长度异常，请确认是否正确配置。");
        }

        LOG.info("配置校验通过：JWT 密钥已就绪（{} 位），AI 密钥{}。",
                jwtSecret.length(), StringUtils.hasText(apiKey) ? "已配置" : "未配置");
    }
}
