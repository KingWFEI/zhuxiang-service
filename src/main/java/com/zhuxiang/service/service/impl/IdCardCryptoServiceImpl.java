package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.service.IdCardCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 身份证加解密实现。
 * <p>
 * 密文格式：v1:Base64(12-byte IV + ciphertext + 16-byte GCM tag)
 */
@Service
public class IdCardCryptoServiceImpl implements IdCardCryptoService {

    private static final Logger log = LoggerFactory.getLogger(IdCardCryptoServiceImpl.class);

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_LENGTH = 32;
    private static final String VERSION_PREFIX = "v1:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] key;

    public IdCardCryptoServiceImpl(@Value("${security.real-name.id-card-key:}") String keyBase64) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            log.info("REAL_NAME_ID_CARD_KEY 未配置，身份证加解密服务将不可用");
            this.key = null;
        } else {
            byte[] decoded = Base64.getDecoder().decode(keyBase64);
            if (decoded.length != AES_KEY_LENGTH) {
                log.error("REAL_NAME_ID_CARD_KEY Base64 解码后长度 {} 不正确（需要 32 字节）", decoded.length);
                this.key = null;
            } else {
                this.key = decoded;
            }
        }
    }

    @Override
    public String encrypt(String plaintext) {
        requireKey();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + ciphertextWithTag
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

            return VERSION_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BusinessException(500, "身份证加密失败");
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        requireKey();
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new BusinessException(400, "密文为空");
        }
        if (!ciphertext.startsWith(VERSION_PREFIX)) {
            throw new BusinessException(400, "不支持的密文版本");
        }

        try {
            String payload = ciphertext.substring(VERSION_PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(payload);

            if (combined.length < GCM_IV_LENGTH + 1) {
                throw new BusinessException(400, "密文格式错误");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "身份证解密失败，请检查密钥或密文完整性");
        }
    }

    @Override
    public String mask(String idCardNo) {
        if (idCardNo == null || idCardNo.length() < 10) {
            return idCardNo;
        }
        return idCardNo.substring(0, 6) + "********" + idCardNo.substring(idCardNo.length() - 4);
    }

    private void requireKey() {
        if (key == null) {
            throw BusinessException.badRequest("身份证加密密钥未配置，请设置 REAL_NAME_ID_CARD_KEY 环境变量");
        }
    }
}
