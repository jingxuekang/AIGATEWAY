package com.aigateway.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * PII 脱敏服务
 * 支持：手机号、身份证、银行卡号、邮箱、IP 地址、API Key
 */
@Slf4j
@Service
public class PiiMaskService {

    @Value("${gateway.pii.enabled:true}")
    private boolean piiEnabled;

    private static final Pattern PHONE     = Pattern.compile("(1[3-9]\\d{9})");
    private static final Pattern ID_CARD   = Pattern.compile("([1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]|[1-9]\\d{14})");
    private static final Pattern BANK_CARD = Pattern.compile("([3-6]\\d{15,18})");
    private static final Pattern EMAIL     = Pattern.compile("([a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})");
    private static final Pattern IPV4      = Pattern.compile("((?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?))");
    private static final Pattern API_KEY   = Pattern.compile("(sk-[a-zA-Z0-9]{8})[a-zA-Z0-9]+");

    /**
     * 对文本进行 PII 脱敏
     */
    public String mask(String text) {
        if (!piiEnabled || text == null || text.isBlank()) return text;

        String result = text;

        // 手机号：保留前3位后4位，中间替换为****
        result = PHONE.matcher(result).replaceAll(m -> {
            String s = m.group(1);
            return s.substring(0, 3) + "****" + s.substring(7);
        });

        // 身份证：保留前6位后4位
        result = ID_CARD.matcher(result).replaceAll(m -> {
            String s = m.group(1);
            if (s.length() == 18) return s.substring(0, 6) + "********" + s.substring(14);
            return s.substring(0, 6) + "*****" + s.substring(11);
        });

        // 银行卡：保留前4位后4位
        result = BANK_CARD.matcher(result).replaceAll(m -> {
            String s = m.group(1);
            int len = s.length();
            return s.substring(0, 4) + "*".repeat(len - 8) + s.substring(len - 4);
        });

        // 邮箱：用户名部分保留首字符
        result = EMAIL.matcher(result).replaceAll(m -> {
            String s = m.group(1);
            int atIdx = s.indexOf('@');
            return s.charAt(0) + "***" + s.substring(atIdx);
        });

        // IP：最后一段替换为 ***
        result = IPV4.matcher(result).replaceAll(m -> {
            String s = m.group(1);
            return s.substring(0, s.lastIndexOf('.') + 1) + "***";
        });

        // API Key：只保留 sk-XXXXXXXX 前缀
        result = API_KEY.matcher(result).replaceAll(m -> m.group(1) + "****...");

        return result;
    }

    /**
     * 对日志内容进行脱敏（PII 脱敏 + 截断）
     */
    public String maskForLog(String content, int maxLength) {
        if (content == null) return null;
        String masked = mask(content);
        return masked.length() > maxLength ? masked.substring(0, maxLength) + "...[truncated]" : masked;
    }
}
