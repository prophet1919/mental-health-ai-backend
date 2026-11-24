package com.wzu.aispringboot.controller;

import com.wzu.aispringboot.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：确认数据库可达、并回显当前使用的上下文存储实现。
 * 不需要鉴权，便于启动后第一时间排查配置问题。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger LOG = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url:unknown}")
    private String datasourceUrl;

    @Value("${app.chat-memory.store:mysql}")
    private String memoryStore;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("memoryStore", memoryStore);
        data.put("database", maskUrl(datasourceUrl));

        try {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            data.put("dbReachable", one != null && one == 1);
        } catch (Exception e) {
            LOG.warn("健康检查：数据库不可达", e);
            data.put("dbReachable", false);
            data.put("status", "DEGRADED");
            data.put("dbError", e.getClass().getSimpleName());
        }
        return Result.ok(data);
    }

    /** 隐藏 URL 中的账号密码等敏感段，仅保留库名 */
    private String maskUrl(String url) {
        if (url == null || url.isBlank()) {
            return "unknown";
        }
        // 先切掉查询串，避免 serverTimezone=Asia/Shanghai 里的斜杠干扰定位
        int question = url.indexOf('?');
        String base = question > 0 ? url.substring(0, question) : url;
        int slash = base.lastIndexOf('/');
        if (slash > 0 && slash < base.length() - 1) {
            return "jdbc:mysql://.../" + base.substring(slash + 1);
        }
        return "jdbc:mysql://...";
    }
}
