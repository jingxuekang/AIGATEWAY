package com.aigateway.admin.controller;

import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Security Settings", description = "安全策略配置")
@RestController
@RequestMapping("/api/admin/security-settings")
@RequiredArgsConstructor
public class SecuritySettingController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "获取安全配置")
    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> map = new HashMap<>();
        map.put("gateway.rate-limit.enabled", getValue("gateway.rate-limit.enabled"));
        map.put("gateway.rate-limit.ip-limit-enabled", getValue("gateway.rate-limit.ip-limit-enabled"));
        map.put("gateway.prompt-guard.enabled", getValue("gateway.prompt-guard.enabled"));
        map.put("gateway.prompt-guard.sensitive-words-enabled", getValue("gateway.prompt-guard.sensitive-words-enabled"));
        return Result.success(map);
    }

    @Data
    public static class SecuritySettingsDTO {
        private Boolean rateLimitEnabled;
        private Boolean ipLimitEnabled;
        private Boolean promptGuardEnabled;
        private Boolean sensitiveWordsEnabled;
    }

    @Operation(summary = "保存安全配置")
    @PostMapping
    public Result<Boolean> saveSettings(@RequestBody SecuritySettingsDTO dto) {
        upsert("gateway.rate-limit.enabled", boolToString(dto.getRateLimitEnabled()));
        upsert("gateway.rate-limit.ip-limit-enabled", boolToString(dto.getIpLimitEnabled()));
        upsert("gateway.prompt-guard.enabled", boolToString(dto.getPromptGuardEnabled()));
        upsert("gateway.prompt-guard.sensitive-words-enabled", boolToString(dto.getSensitiveWordsEnabled()));
        return Result.success(true);
    }

    private String getValue(String key) {
        String sql = "SELECT config_value FROM system_setting WHERE config_key = ?";
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null, key);
    }

    private void upsert(String key, String value) {
        if (value == null) return;
        int updated = jdbcTemplate.update(
                "UPDATE system_setting SET config_value=? WHERE config_key=?",
                value, key
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO system_setting(config_key, config_value) VALUES (?,?)",
                    key, value
            );
        }
    }

    private String boolToString(Boolean b) {
        return b == null ? null : (b ? "true" : "false");
    }
}

