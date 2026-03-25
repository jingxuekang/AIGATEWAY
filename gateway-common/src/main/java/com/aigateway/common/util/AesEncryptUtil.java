package com.aigateway.common.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256-CBC 加密工具
 *
 * 加密原理：
 *   1. 主密码 + 随机盐值(16字节) -> PBKDF2(65536次迭代) -> 256位AES密钥
 *   2. 随机IV(16字节) + AES-256-CBC -> 密文
 *   3. Base64(盐值 + IV + 密文) -> 最终密文字符串，包装为 ENC(...)
 *
 * 解密原理：
 *   1. Base64解码 -> 拆分盐值(前16字节) + IV(16-32字节) + 密文(32字节后)
 *   2. 主密码 + 盐值 -> PBKDF2 -> AES密钥
 *   3. AES-256-CBC解密 -> 明文
 *
 * 安全性：
 *   - 每次加密产生不同密文（随机盐+随机IV），防止彩虹表攻击
 *   - PBKDF2 65536次迭代，防止暴力破解
 *   - 主密码通过环境变量 GATEWAY_MASTER_KEY 传入，不存储在代码中
 */
public class AesEncryptUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    /**
     * 加密明文，返回 Base64 密文
     */
    public static String encrypt(String plainText, String masterKey) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        SecretKey key = deriveKey(masterKey, salt);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[SALT_LENGTH + IV_LENGTH + encrypted.length];
        System.arraycopy(salt, 0, combined, 0, SALT_LENGTH);
        System.arraycopy(iv, 0, combined, SALT_LENGTH, IV_LENGTH);
        System.arraycopy(encrypted, 0, combined, SALT_LENGTH + IV_LENGTH, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 解密 Base64 密文，返回明文
     */
    public static String decrypt(String cipherText, String masterKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(cipherText);

        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[combined.length - SALT_LENGTH - IV_LENGTH];

        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
        System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH);
        System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, encrypted, 0, encrypted.length);

        SecretKey key = deriveKey(masterKey, salt);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    /** 判断是否为 ENC(...) 格式 */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith("ENC(") && value.endsWith(")");
    }

    /** 从 ENC(密文) 提取密文部分 */
    public static String unwrap(String value) {
        return value.substring(4, value.length() - 1);
    }

    /** 将密文包装为 ENC(密文) */
    public static String wrap(String cipherText) {
        return "ENC(" + cipherText + ")";
    }

    private static SecretKey deriveKey(String masterKey, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(masterKey.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 命令行加密工具
     * 用法: java -cp target/classes com.aigateway.common.util.AesEncryptUtil <明文> <主密码>
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java AesEncryptUtil <plaintext> <masterKey>");
            return;
        }
        String encrypted = encrypt(args[0], args[1]);
        System.out.println("ENC Value: " + wrap(encrypted));
        String decrypted = decrypt(encrypted, args[1]);
        System.out.println("Verify OK: " + args[0].equals(decrypted));
    }
}
