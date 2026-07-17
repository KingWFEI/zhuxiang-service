package com.zhuxiang.service.service;

/**
 * 身份证加解密服务。
 * <p>
 * 使用 AES-256-GCM 加密，密文格式为 {@code v1:<Base64(IV + ciphertextWithTag)>}。
 */
public interface IdCardCryptoService {

    /**
     * 加密身份证明文。
     *
     * @param plaintext 身份证明文
     * @return 带版本前缀的密文
     */
    String encrypt(String plaintext);

    /**
     * 解密密文。
     *
     * @param ciphertext 带版本前缀的密文
     * @return 身份证明文
     */
    String decrypt(String ciphertext);

    /**
     * 身份证号码脱敏：前 6 位 + 8 个星号 + 后 4 位。
     *
     * @param idCardNo 完整身份证号
     * @return 脱敏后字符串
     */
    String mask(String idCardNo);
}
