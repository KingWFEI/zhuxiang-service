package com.zhuxiang.service;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.service.IdCardCryptoService;
import com.zhuxiang.service.service.impl.IdCardCryptoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 身份证加解密服务单元测试。
 * <p>
 * 测试密钥为随机生成的 32 字节 Base64 密钥，不使用真实密钥。
 * 测试身份证号均为虚构。
 */
class IdCardCryptoServiceTests {

    private static final String TEST_PLAINTEXT = "110101199001010000";

    private IdCardCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        String keyBase64 = Base64.getEncoder().encodeToString(key);
        cryptoService = new IdCardCryptoServiceImpl(keyBase64);
    }

    @Test
    void encrypt_shouldProduceDifferentCiphertextsForSamePlaintext() {
        String c1 = cryptoService.encrypt(TEST_PLAINTEXT);
        String c2 = cryptoService.encrypt(TEST_PLAINTEXT);
        assertThat(c1).isNotEqualTo(c2); // 每次不同 IV
    }

    @Test
    void encryptThenDecrypt_shouldReturnOriginalPlaintext() {
        String ciphertext = cryptoService.encrypt(TEST_PLAINTEXT);
        String decrypted = cryptoService.decrypt(ciphertext);
        assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
    }

    @Test
    void decrypt_shouldFailForTamperedCiphertext() {
        String ciphertext = cryptoService.encrypt(TEST_PLAINTEXT);
        // Tamper the Base64 payload (after "v1:"), not the version prefix
        String prefix = "v1:";
        String payload = ciphertext.substring(prefix.length());
        String tampered = prefix + payload.substring(0, payload.length() - 4) + "XXXX";
        assertThatThrownBy(() -> cryptoService.decrypt(tampered))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void decrypt_shouldFailForWrongKey() {
        String ciphertext = cryptoService.encrypt(TEST_PLAINTEXT);

        // 创建不同密钥的服务
        byte[] wrongKey = new byte[32];
        new java.security.SecureRandom().nextBytes(wrongKey);
        String wrongKeyBase64 = Base64.getEncoder().encodeToString(wrongKey);
        IdCardCryptoService wrongService = new IdCardCryptoServiceImpl(wrongKeyBase64);

        assertThatThrownBy(() -> wrongService.decrypt(ciphertext))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("解密失败");
    }

    @Test
    void decrypt_shouldRejectWrongVersion() {
        String ciphertext = cryptoService.encrypt(TEST_PLAINTEXT);
        String badVersion = "v2:" + ciphertext.substring(3);
        assertThatThrownBy(() -> cryptoService.decrypt(badVersion))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("版本");
    }

    @Test
    void service_shouldFailWhenKeyNot32Bytes() {
        String badKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new IdCardCryptoServiceImpl(badKey).encrypt(TEST_PLAINTEXT))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void service_shouldFailWhenKeyEmpty() {
        IdCardCryptoService service = new IdCardCryptoServiceImpl("");
        assertThatThrownBy(() -> service.encrypt(TEST_PLAINTEXT))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void service_shouldNotFailStartupWhenKeyEmpty() {
        IdCardCryptoService service = new IdCardCryptoServiceImpl("");
        // 构造不应失败，只在调用时抛异常
        assertThat(service).isNotNull();
    }

    @Test
    void mask_shouldKeepFirst6AndLast4() {
        String masked = cryptoService.mask("110101199001010000");
        assertThat(masked).isEqualTo("110101********0000");
        assertThat(masked).doesNotContain("19900101");
    }

    @Test
    void mask_shouldHandleShortInput() {
        assertThat(cryptoService.mask("123")).isEqualTo("123");
        assertThat(cryptoService.mask(null)).isNull();
    }

    @Test
    void ciphertext_shouldStartWithVersionPrefix() {
        String ciphertext = cryptoService.encrypt(TEST_PLAINTEXT);
        assertThat(ciphertext).startsWith("v1:");
    }

    @Test
    void encrypt_shouldNotContainPlaintextInCiphertext() {
        String ciphertext = cryptoService.encrypt(TEST_PLAINTEXT);
        assertThat(ciphertext).doesNotContain("110101");
        assertThat(ciphertext).doesNotContain("19900101");
    }
}
