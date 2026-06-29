package com.zhuxiang.service.security;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.LockPasscodeProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 使用 AES-GCM 对期限密码进行带认证的可逆加密。
 */
@Component
public class LockPasscodeCrypto {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final LockPasscodeProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public LockPasscodeCrypto(LockPasscodeProperties properties) {
        this.properties = properties;
    }

    /** 在调用外部平台前校验密钥，避免平台已生成密码但本地无法加密。 */
    public void validateConfiguration() {
        currentKey();
        requireKeyVersion();
    }

    /**
     * 加密密码，密文封装中包含密钥版本、nonce 以及带认证标签的密文。
     */
    public String encrypt(String passcode, String context) {
        if (!StringUtils.hasText(passcode)) {
            throw BusinessException.badRequest("门锁密码不能为空");
        }
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, currentKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(passcode.getBytes(StandardCharsets.UTF_8));
            return "v" + requireKeyVersion() + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce) + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("门锁密码加密失败", exception);
        }
    }

    /**
     * 校验密钥版本并解密密码；认证标签不匹配时拒绝返回数据。
     */
    public String decrypt(String ciphertext, String context) {
        if (!StringUtils.hasText(ciphertext)) {
            throw BusinessException.conflict("门锁密码密文不存在");
        }
        try {
            String[] parts = ciphertext.split(":", 3);
            if (parts.length != 3 || !parts[0].equals("v" + requireKeyVersion())) {
                throw BusinessException.conflict("门锁密码密钥版本不可用");
            }
            byte[] nonce = Base64.getUrlDecoder().decode(parts[1]);
            if (nonce.length != NONCE_LENGTH) {
                throw BusinessException.conflict("门锁密码密文格式无效");
            }
            byte[] encrypted = Base64.getUrlDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, currentKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BusinessException exception) {
            throw exception;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw BusinessException.conflict("门锁密码密文校验失败");
        }
    }

    /** 读取并校验当前 AES 密钥。 */
    private SecretKeySpec currentKey() {
        if (!StringUtils.hasText(properties.getEncryptionKey())) {
            throw new IllegalStateException("LOCK_PASSCODE_ENCRYPTION_KEY 未配置");
        }
        byte[] key = Base64.getDecoder().decode(properties.getEncryptionKey().trim());
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalStateException("LOCK_PASSCODE_ENCRYPTION_KEY 必须是 16、24 或 32 字节 AES 密钥的 Base64 编码");
        }
        return new SecretKeySpec(key, "AES");
    }

    /** 读取当前密钥版本。 */
    private String requireKeyVersion() {
        if (!StringUtils.hasText(properties.getKeyVersion())) {
            throw new IllegalStateException("LOCK_PASSCODE_KEY_VERSION 未配置");
        }
        return properties.getKeyVersion().trim();
    }
}
