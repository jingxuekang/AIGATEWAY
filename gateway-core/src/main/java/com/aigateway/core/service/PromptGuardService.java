package com.aigateway.core.service;

import com.aigateway.common.exception.BusinessException;
import com.aigateway.provider.model.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PromptGuardService {

    @Value("${gateway.prompt-guard.enabled:true}")
    private boolean enabled;

    @Value("${gateway.prompt-guard.max-message-length:100000}")
    private int maxMessageLength;

    @Value("${gateway.prompt-guard.max-messages:100}")
    private int maxMessages;

    @Value("${gateway.prompt-guard.regex-timeout-ms:100}")
    private long regexTimeoutMs;

    @Value("${gateway.prompt-guard.sensitive-words-enabled:true}")
    private boolean sensitiveWordsEnabled;

    private static final ExecutorService REGEX_EXECUTOR =
            Executors.newFixedThreadPool(
                    Math.min(8, Runtime.getRuntime().availableProcessors() * 2),
                    r -> { Thread t = new Thread(r, "regex-guard"); t.setDaemon(true); return t; });

    private static final List<Pattern> EN_INJECTION_PATTERNS = List.of(
        Pattern.compile("ignore\\s+(?:all\\s+)?(?:previous|above|prior)\\s+(?:instructions?|prompts?|rules?)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("disregard\\s+(?:all\\s+)?(?:previous|above|prior)\\s+(?:instructions?|prompts?)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("forget\\s+(?:all\\s+)?(?:previous|above|prior|your)\\s+(?:instructions?|rules?|training)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("you\\s+are\\s+now\\s+(?:a\\s+)?(?:dan|jailbreak|evil|unrestricted)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjailbreak\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bDAN\\b"),
        Pattern.compile("act\\s+as\\s+(?:if\\s+you\\s+are\\s+|a\\s+)?(?:evil|malicious|unrestricted|uncensored)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:print|show|reveal|display|output)\\s+(?:your\\s+)?(?:system\\s+prompt|initial\\s+instructions?)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("pretend\\s+(?:you\\s+)?(?:are|have\\s+no)\\s+(?:a\\s+)?(?:restrictions?|rules?|limits?|filters?)",
            Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> ZH_INJECTION_PATTERNS = List.of(
        Pattern.compile("(?:\u5ffd\u7565|\u65e0\u89c6|\u4e0d\u7ba1|\u4e0d\u987e)(?:\u6240\u6709|\u5168\u90e8|\u4e4b\u524d|\u4e0a\u9762|\u524d\u9762|\u4ee5\u524d)?(?:\u7684)?(?:\u6307\u4ee4|\u63d0\u793a|\u89c4\u5219|\u9650\u5236|\u7ea6\u675f)"),
        Pattern.compile("(?:\u5fd8\u8bb0|\u6e05\u9664|\u629b\u5f03|\u4e22\u5f03)(?:\u4f60|\u60a8)?(?:\u4e4b\u524d|\u4ee5\u524d|\u539f\u6765|\u6240\u6709)?(?:\u7684)?(?:\u6307\u4ee4|\u8bbe\u5b9a|\u8bad\u7ec3|\u89c4\u5219|\u9650\u5236)"),
        Pattern.compile("\u4f60(?:\u73b0\u5728)?(?:\u662f|\u53d8\u6210|\u6210\u4e3a)(?:\u4e00\u4e2a)?(?:\u6ca1\u6709\u9650\u5236|\u4e0d\u53d7\u7ea6\u675f|\u81ea\u7531|\u8d8a\u72f1|\u90aa\u6076|\u6076\u610f)(?:\u7684)?(?:AI|\u4eba\u5de5\u667a\u80fd|\u52a9\u624b|\u6a21\u578b)?"),
        Pattern.compile("\u8d8a\u72f1"),
        Pattern.compile("\u5047\u88c5(?:\u4f60|\u60a8)?(?:\u6ca1\u6709|\u4e0d\u53d7|\u65e0\u89c6)(?:\u4efb\u4f55)?(?:\u9650\u5236|\u89c4\u5219|\u7ea6\u675f|\u8fc7\u6ee4)"),
        Pattern.compile("(?:\u544a\u8bc9\u6211|\u663e\u793a|\u8f93\u51fa|\u6253\u5370|\u6cc4\u9732)(?:\u4f60|\u60a8)?(?:\u7684)?(?:\u7cfb\u7edf\u63d0\u793a\u8bcd?|\u521d\u59cb\u6307\u4ee4|\u539f\u59cb\u8bbe\u5b9a|\u63d0\u793a\u8bcd?)"),
        Pattern.compile("(?:\u4ee5|\u7528|\u8fdb\u5165|\u5f00\u542f|\u542f\u52a8)?(?:DAN|\u8d8a\u72f1|\u65e0\u9650\u5236|\u65e0\u5ba1\u67e5)(?:\u6a21\u5f0f|\u8eab\u4efd|\u89d2\u8272)?"),
        Pattern.compile("\u626e\u6f14(?:\u4e00\u4e2a)?(?:\u4e0d\u53d7\u7ea6\u675f|\u6ca1\u6709\u9650\u5236|\u90aa\u6076|\u6076\u610f|\u65e0\u5ba1\u67e5)(?:\u7684)?(?:AI|\u4eba\u5de5\u667a\u80fd|\u52a9\u624b|\u89d2\u8272)?")
    );

    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
        Pattern.compile("(?:drop\\s+table|delete\\s+from|insert\\s+into|union\\s+select)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:leak|extract|dump|exfiltrate)\\s+(?:the\\s+)?(?:prompt|system|instructions?|training)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:write|create|generate|code)\\s+(?:a\\s+)?(?:virus|malware|ransomware|trojan|rootkit|keylogger)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:porn|pornography|xxx\\s*video?s?|sex\\s*video?s?|nude\\s*pics?|nsfw\\s*content)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:terrorist\\s+attack|how\\s+to\\s+make\\s+a\\s+bomb|improvised\\s+explosive\\s+device)",
            Pattern.CASE_INSENSITIVE)
    );

    public void check(ChatRequest request) {
        if (!enabled) return;
        if (request.getMessages() == null) return;

        if (request.getMessages().size() > maxMessages) {
            log.warn("[PromptGuard] Too many messages: count={}", request.getMessages().size());
            throw new BusinessException(400, "Too many messages. Maximum allowed: " + maxMessages);
        }

        for (ChatRequest.Message msg : request.getMessages()) {
            if (msg.getContent() == null) continue;
            String contentStr = msg.getContentAsString();
            if (contentStr == null) continue;

            if (contentStr.length() > maxMessageLength) {
                log.warn("[PromptGuard] Message too long: length={}, role={}",
                    contentStr.length(), msg.getRole());
                throw new BusinessException(400,
                    "Message too long. Maximum allowed: " + maxMessageLength + " characters.");
            }

            if (!"system".equals(msg.getRole())) {
                checkPatterns(contentStr, msg.getRole(), EN_INJECTION_PATTERNS, "EN_INJECTION");
                checkPatterns(contentStr, msg.getRole(), ZH_INJECTION_PATTERNS, "ZH_INJECTION");
            }

            if (sensitiveWordsEnabled) {
                checkPatterns(contentStr, msg.getRole(), SENSITIVE_PATTERNS, "SENSITIVE");
            }
        }
    }

    private void checkPatterns(String content, String role, List<Pattern> patterns, String type) {
        for (Pattern pattern : patterns) {
            boolean matched;
            try {
                matched = matchWithTimeout(pattern, content);
            } catch (TimeoutException e) {
                log.warn("[PromptGuard] Regex timeout: type={}, role={}, rejecting", type, role);
                throw new BusinessException(400, "Request rejected: message processing timeout.");
            }
            if (matched) {
                log.warn("[PromptGuard] {} detected: role={}, pattern={}", type, role, pattern.pattern());
                if ("SENSITIVE".equals(type)) {
                    throw new BusinessException(400, "Request rejected: sensitive content detected.");
                } else {
                    throw new BusinessException(400, "Request rejected: potential prompt injection detected.");
                }
            }
        }
    }

    private boolean matchWithTimeout(Pattern pattern, String content) throws TimeoutException {
        Future<Boolean> future = REGEX_EXECUTOR.submit(() -> {
            Matcher matcher = pattern.matcher(content);
            return matcher.find();
        });
        try {
            return future.get(regexTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Regex match timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new TimeoutException("Regex match interrupted");
        } catch (ExecutionException e) {
            return false;
        }
    }
}
