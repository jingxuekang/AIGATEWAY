package com.aigateway.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @deprecated 已由 Jasypt（jasypt-spring-boot-starter）替代。
 * 原来识别 AES{...} / ENC(...) 格式的自研加密方案已弃用。
 * 当前加密方案：jasypt.encryptor.password + PBEWithMD5AndDES 算法。
 * 本类保留仅供参考，不再注册到 spring.factories，不会在启动时执行。
 */
@Deprecated(since = "1.0.0", forRemoval = true)
@Slf4j
public class AdminEncryptionPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        // 已弃用，不执行任何操作
        log.debug("[AdminEncryptionPostProcessor] Deprecated, no-op. Use jasypt instead.");
    }
}
