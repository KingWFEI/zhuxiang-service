package com.zhuxiang.service;

import com.zhuxiang.service.client.EsignRequestSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * e签宝请求签名工具单元测试。
 * <p>
 * 所有测试使用假 AppId/AppSecret/姓名/证件号。
 */
class EsignRequestSignerTests {

    private static final String FAKE_SECRET = "SECRET_FOR_UNIT_TEST_ONLY";
    private final EsignRequestSigner signer = new EsignRequestSigner();

    @Test
    void computeContentMd5_shouldBeStableForSameInput() {
        String body = "{\"name\":\"测试用户\"}";

        String md5a = signer.computeContentMd5(body);
        String md5b = signer.computeContentMd5(body);

        assertThat(md5a).isEqualTo(md5b);
        assertThat(md5a).isNotBlank();
    }

    @Test
    void computeContentMd5_shouldChangeWhenBodyDiffers() {
        String body1 = "{\"name\":\"测试用户\"}";
        String body2 = "{\"name\":\"测试用户 \"}";

        String md5a = signer.computeContentMd5(body1);
        String md5b = signer.computeContentMd5(body2);

        assertThat(md5a).isNotEqualTo(md5b);
    }

    @Test
    void computeContentMd5_shouldChangeWithExtraWhitespace() {
        String body1 = "{\"name\":\"测试用户\"}";
        String body2 = "{\"name\": \"测试用户\"}";

        String md5a = signer.computeContentMd5(body1);
        String md5b = signer.computeContentMd5(body2);

        assertThat(md5a).isNotEqualTo(md5b);
    }

    @Test
    void postStringToSign_shouldHaveCorrectNewlineCount() {
        String path = "/v2/identity/auth/api/individual/face";
        String contentMd5 = "test-md5";

        String sts = signer.buildPostStringToSign(path, contentMd5);

        String[] lines = sts.split("\n", -1);
        assertThat(lines).hasSize(6);
        assertThat(lines[0]).isEqualTo("POST");
        assertThat(lines[1]).isEqualTo("*/*");
        assertThat(lines[2]).isEqualTo("test-md5");
        assertThat(lines[3]).isEqualTo("application/json; charset=UTF-8");
        assertThat(lines[4]).isEqualTo("");
        assertThat(lines[5]).isEqualTo(path);
    }

    @Test
    void getStringToSign_shouldPreserveAllEmptyLines() {
        String path = "/v2/identity/auth/api/common/123456789/detail";

        String sts = signer.buildGetStringToSign(path);

        String[] lines = sts.split("\n", -1);
        assertThat(lines).hasSize(6);
        assertThat(lines[0]).isEqualTo("GET");
        assertThat(lines[1]).isEqualTo("*/*");
        assertThat(lines[2]).isEqualTo(""); // Content-MD5 = ""
        assertThat(lines[3]).isEqualTo(""); // Content-Type = ""
        assertThat(lines[4]).isEqualTo(""); // empty line
        assertThat(lines[5]).isEqualTo(path);
    }

    @Test
    void getStringToSign_shouldNotContainContentMd5() {
        String path = "/v2/identity/auth/api/common/123456/detail";

        String sts = signer.buildGetStringToSign(path);

        assertThat(sts).doesNotContain("Content-MD5");
        assertThat(sts).doesNotContain("Content-Type");
    }

    @Test
    void path_shouldNotContainHostname() {
        String path = "/v2/identity/auth/api/individual/face";
        String contentMd5 = signer.computeContentMd5("{}");

        String sts = signer.buildPostStringToSign(path, contentMd5);

        assertThat(sts).doesNotContain("https://");
        assertThat(sts).doesNotContain("http://");
        assertThat(sts).doesNotContain("smlopenapi.esign.cn");
        assertThat(sts).doesNotContain("openapi.esign.cn");
    }

    @Test
    void signature_shouldBeStableWithFixedInputs() {
        String body = "{\"name\":\"测试用户\"}";
        String contentMd5 = signer.computeContentMd5(body);
        String stringToSign = signer.buildPostStringToSign(
                "/v2/identity/auth/api/individual/face", contentMd5);

        String sig1 = signer.sign(FAKE_SECRET, stringToSign);
        String sig2 = signer.sign(FAKE_SECRET, stringToSign);

        assertThat(sig1).isEqualTo(sig2);
        assertThat(sig1).isNotBlank();
    }

    @Test
    void signature_shouldChangeWithDifferentSecret() {
        String body = "{\"name\":\"测试用户\"}";
        String contentMd5 = signer.computeContentMd5(body);
        String sts = signer.buildPostStringToSign(
                "/v2/identity/auth/api/individual/face", contentMd5);

        String sig1 = signer.sign("SECRET_A", sts);
        String sig2 = signer.sign("SECRET_B", sts);

        assertThat(sig1).isNotEqualTo(sig2);
    }
}
