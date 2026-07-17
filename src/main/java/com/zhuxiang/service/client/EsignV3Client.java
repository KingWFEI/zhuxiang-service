package com.zhuxiang.service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.EsignException;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.dto.LeaseContractFillData;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * e签宝 V3 电子合同客户端。
 * 签名复用 {@link EsignRequestSigner}，Body 只序列化一次。
 */
@Component
public class EsignV3Client {

    private static final Logger log = LoggerFactory.getLogger(EsignV3Client.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    private final RestTemplate restTemplate;
    private final EsignV3Properties properties;
    private final ObjectMapper objectMapper;
    private final EsignRequestSigner signer;

    public EsignV3Client(RestTemplate restTemplate, EsignV3Properties properties,
                         ObjectMapper objectMapper, EsignRequestSigner signer) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.signer = signer;
    }

    // ==================== 接口一：查询模板详情 ====================

    public TemplateDetailResponse getTemplateDetail(String docTemplateId) {
        requireConfigured();
        String path = "/v3/doc-templates/" + docTemplateId;
        return get(path, TemplateDetailResponse.class);
    }

    // ==================== 接口二：填写模板生成合同 ====================

    public CreateFileResponse createByDocTemplate(LeaseContractFillData data, String docTemplateId) {
        requireConfigured();

        List<Component> components = buildComponents(data);
        CreateFileRequest req = new CreateFileRequest();
        req.setDocTemplateId(docTemplateId);
        req.setFileName("租房合同.pdf");
        req.setRequiredCheck(true);
        req.setComponents(components);

        String path = "/v3/files/create-by-doc-template";
        CreateFileResponse resp = post(path, req, CreateFileResponse.class);

        if (resp.getCode() != 0) {
            throw EsignException.signingFailed(String.valueOf(resp.getCode()),
                    resp.getMessage(), path);
        }
        log.info("e签宝生成合同成功: fileId={}", resp.getData() != null ? resp.getData().getFileId() : "null");
        return resp;
    }

    // ==================== 接口三：发起签署流程 ====================

    public CreateSignFlowResponse createSignFlow(String contractFileId,
                                                  LeaseContractFillData fillData) {
        requireConfigured();

        CreateSignFlowRequest req = buildSignFlowRequest(contractFileId, fillData);
        String path = "/v3/sign-flow/create-by-file";
        CreateSignFlowResponse resp = post(path, req, CreateSignFlowResponse.class);

        if (resp.getCode() != 0) {
            throw EsignException.signingFailed(String.valueOf(resp.getCode()),
                    resp.getMessage(), path);
        }
        log.info("e签宝发起签署成功: signFlowId={}", resp.getData() != null ? resp.getData().getSignFlowId() : "null");
        return resp;
    }

    // ==================== 接口四：获取签署链接 ====================

    public SignUrlResponse getSignUrl(String signFlowId, String psnAccount) {
        requireConfigured();

        SignUrlRequest req = new SignUrlRequest();
        req.setNeedLogin(false);
        req.setUrlType(2);
        SignUrlRequest.Operator op = new SignUrlRequest.Operator();
        op.setPsnAccount(psnAccount);
        req.setOperator(op);

        String path = "/v3/sign-flow/" + signFlowId + "/sign-url";
        SignUrlResponse resp = post(path, req, SignUrlResponse.class);

        if (resp.getCode() != 0) {
            throw EsignException.signingFailed(String.valueOf(resp.getCode()),
                    resp.getMessage(), path);
        }
        log.info("e签宝获取签署链接成功: signFlowId={}, urlType=2", signFlowId);
        return resp;
    }

    // ==================== 接口五：查询签署状态 ====================

    public SignFlowDetailResponse getSignFlowDetail(String signFlowId) {
        requireConfigured();
        String path = "/v3/sign-flow/" + signFlowId + "/detail";
        SignFlowDetailResponse resp = get(path, SignFlowDetailResponse.class);

        if (resp.getCode() != 0) {
            throw EsignException.signingFailed(String.valueOf(resp.getCode()),
                    resp.getMessage(), path);
        }
        return resp;
    }

    // ==================== 接口六：获取已签合同下载链接 ====================

    public FileDownloadResponse getFileDownloadUrl(String signFlowId) {
        requireConfigured();
        String path = "/v3/sign-flow/" + signFlowId + "/file-download-url";
        FileDownloadResponse resp = get(path, FileDownloadResponse.class);

        if (resp.getCode() != 0) {
            throw EsignException.signingFailed(String.valueOf(resp.getCode()),
                    resp.getMessage(), path);
        }
        return resp;
    }

    // ==================== HTTP 方法 ====================

    private <T> T get(String path, Class<T> responseType) {
        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = signer.buildGetStringToSign(path);
            String signature = signer.sign(properties.getAppSecret(), stringToSign);
            String url = normalizeBaseUrl() + path;

            long start = System.currentTimeMillis();
            String respBody = restTemplate.execute(URI.create(url), HttpMethod.GET,
                    clientRequest -> {
                        clientRequest.getHeaders().set("X-Tsign-Open-App-Id", properties.getAppId());
                        clientRequest.getHeaders().set("X-Tsign-Open-Auth-Mode", "Signature");
                        clientRequest.getHeaders().set("X-Tsign-Open-Ca-Signature", signature);
                        clientRequest.getHeaders().set("X-Tsign-Open-Ca-Timestamp", String.valueOf(timestamp));
                        clientRequest.getHeaders().set("Accept", EsignRequestSigner.ACCEPT);
                    },
                    this::extractResponseBody);
            long elapsed = System.currentTimeMillis() - start;
            log.debug("e签宝 GET {} -> {}ms", path, elapsed);
            return objectMapper.readValue(respBody, responseType);
        } catch (EsignException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new EsignException(0, "NETWORK", "e签宝服务连接超时或网络不可用", path);
        } catch (RestClientException e) {
            throw new EsignException(0, "NETWORK", "e签宝服务请求失败", path);
        } catch (Exception e) {
            throw new EsignException(0, "UNKNOWN", "e签宝调用异常", path);
        }
    }

    private <TReq, TResp> TResp post(String path, TReq request, Class<TResp> responseType) {
        try {
            String rawBody = objectMapper.writeValueAsString(request);
            long timestamp = System.currentTimeMillis();

            String contentMd5 = signer.computeContentMd5(rawBody);
            String stringToSign = signer.buildPostStringToSign(path, contentMd5);
            String signature = signer.sign(properties.getAppSecret(), stringToSign);
            String url = normalizeBaseUrl() + path;

            long start = System.currentTimeMillis();
            String respBody = restTemplate.execute(URI.create(url), HttpMethod.POST,
                    clientRequest -> {
                        clientRequest.getHeaders().set("X-Tsign-Open-App-Id", properties.getAppId());
                        clientRequest.getHeaders().set("X-Tsign-Open-Auth-Mode", "Signature");
                        clientRequest.getHeaders().set("X-Tsign-Open-Ca-Signature", signature);
                        clientRequest.getHeaders().set("X-Tsign-Open-Ca-Timestamp", String.valueOf(timestamp));
                        clientRequest.getHeaders().set("Accept", EsignRequestSigner.ACCEPT);
                        clientRequest.getHeaders().set("Content-Type", EsignRequestSigner.CONTENT_TYPE_JSON);
                        clientRequest.getHeaders().set("Content-MD5", contentMd5);
                        clientRequest.getBody().write(rawBody.getBytes(StandardCharsets.UTF_8));
                    },
                    this::extractResponseBody);
            long elapsed = System.currentTimeMillis() - start;
            log.debug("e签宝 POST {} -> {}ms", path, elapsed);
            return objectMapper.readValue(respBody, responseType);
        } catch (EsignException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new EsignException(0, "NETWORK", "e签宝服务连接超时或网络不可用", path);
        } catch (RestClientException e) {
            throw new EsignException(0, "NETWORK", "e签宝服务请求失败", path);
        } catch (Exception e) {
            throw new EsignException(0, "UNKNOWN", "e签宝调用异常", path);
        }
    }

    // ==================== 工具方法 ====================

    private String normalizeBaseUrl() {
        String url = properties.getBaseUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw BusinessException.badRequest(
                    "e签宝电子合同未配置：请设置 ESIGN_APP_ID 和 ESIGN_APP_SECRET 环境变量");
        }
    }

    private String extractResponseBody(ClientHttpResponse response) throws java.io.IOException {
        byte[] bytes = response.getBody().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // ==================== 控件填充 ====================

    private List<Component> buildComponents(LeaseContractFillData d) {
        List<Component> list = new ArrayList<>();
        String signDate = d.getLessorSignDate() != null ? d.getLessorSignDate().format(DATE_FMT) : "";

        // 1. 甲方手机号
        list.add(comp("5a370264c031478881f3190bc57ed0d1", d.getLessorMobile()));
        // 2. 乙方手机号
        list.add(comp("4d9af6b5635a4ac1b3cac118dbdfc158", d.getTenantMobile()));
        // 3. 甲方签字日期
        list.add(comp("3f2828bb27ce428389b7705d8cee4583", signDate));
        // 4. 乙方签字日期
        list.add(comp("8db2c90bd40848f882e6a11e9e5b8ffb", signDate));
        // 5. 甲方姓名
        list.add(comp("4b7411bae10f4245bd3dcc8966d8aa96", d.getLessorName()));
        // 6. 乙方姓名
        list.add(comp("df752b99852c4dafb55053ed8dbccc4d", d.getTenantName()));
        // 7. 甲方身份证号
        list.add(comp("6076621c6ff841f9ba7785b7f2953c07", d.getLessorIdCard()));
        // 8. 乙方身份证号
        list.add(comp("b02f5f912f2444eaa049ea62e02c5304", d.getTenantIdCard()));
        // 9. 房屋坐落地点（截断到 29 字符）
        String addr = d.getHouseAddress();
        if (addr != null && addr.length() > 29) addr = addr.substring(0, 29);
        list.add(comp("18a756d26a24414394f07d0e0edfa7eb", addr));
        // 10. 租期（月）
        list.add(comp("088601e5dbb44977865b79d172d585c8", String.valueOf(d.getLeaseMonths() != null ? d.getLeaseMonths() : 1)));
        // 11. 起租日期
        list.add(comp("51841a2427d54eacaf98c129025ad8f0", d.getLeaseStartDate() != null ? d.getLeaseStartDate().format(DATE_FMT) : ""));
        // 12. 租期截至日期
        list.add(comp("28a5bc5ef0694366a264816bb196fb18", d.getLeaseEndDate() != null ? d.getLeaseEndDate().format(DATE_FMT) : ""));
        // 13. 不续租提前告知
        list.add(comp("c6830e38f7c14311aa0231c47f03f6cb", String.valueOf(d.getNoticeMonths() != null ? d.getNoticeMonths() : 1)));
        // 14. 押金
        list.add(comp("2cf9ab0ea4e24451b0758fce1e43809f", fmtFen(d.getDeposit())));
        // 15. 租金
        list.add(comp("74fea2206ae446dab401d4ad8782b89a", fmtFen(d.getMonthlyRent())));
        // 16. 租金交付日
        list.add(comp("51a4799042384cb095728c8f38e76a6f", d.getRentPaymentDate() != null ? d.getRentPaymentDate().format(DATE_FMT) : ""));
        return list;
    }

    private CreateSignFlowRequest buildSignFlowRequest(String contractFileId, LeaseContractFillData d) {
        CreateSignFlowRequest req = new CreateSignFlowRequest();

        // docs
        CreateSignFlowRequest.Doc doc = new CreateSignFlowRequest.Doc();
        doc.setFileId(contractFileId);
        doc.setFileName("租房合同.pdf");
        req.setDocs(List.of(doc));

        // signFlowConfig
        CreateSignFlowRequest.SignFlowConfig cfg = new CreateSignFlowRequest.SignFlowConfig();
        cfg.setSignFlowTitle("租房合同签署");
        cfg.setAutoFinish(properties.isAutoFinish());
        req.setSignFlowConfig(cfg);

        // 甲方签署人
        CreateSignFlowRequest.Signer lessor = buildSigner(d.getLessorName(), d.getLessorMobile(), d.getLessorIdCard(),
                "lessor_sign_001", contractFileId, 289.06412, 194.35297);
        // 乙方签署人
        CreateSignFlowRequest.Signer tenant = buildSigner(d.getTenantName(), d.getTenantMobile(), d.getTenantIdCard(),
                "tenant_sign_001", contractFileId, 459.49606, 193.78763);

        req.setSigners(List.of(lessor, tenant));
        return req;
    }

    private CreateSignFlowRequest.Signer buildSigner(String name, String mobile, String idCard,
                                                      String customBizNum, String fileId,
                                                      double posX, double posY) {
        CreateSignFlowRequest.Signer s = new CreateSignFlowRequest.Signer();

        CreateSignFlowRequest.SignConfig sc = new CreateSignFlowRequest.SignConfig();
        sc.setSignOrder(1);
        s.setSignConfig(sc);

        CreateSignFlowRequest.NoticeConfig nc = new CreateSignFlowRequest.NoticeConfig();
        nc.setNoticeTypes("");
        s.setNoticeConfig(nc);

        s.setSignerType(0);

        CreateSignFlowRequest.PsnSignerInfo psi = new CreateSignFlowRequest.PsnSignerInfo();
        psi.setPsnAccount(mobile);
        CreateSignFlowRequest.PsnInfo pi = new CreateSignFlowRequest.PsnInfo();
        pi.setPsnName(name);
        pi.setPsnIDCardNum(idCard);
        pi.setPsnIDCardType("CRED_PSN_CH_IDCARD");
        psi.setPsnInfo(pi);
        s.setPsnSignerInfo(psi);

        CreateSignFlowRequest.SignField sf = new CreateSignFlowRequest.SignField();
        sf.setCustomBizNum(customBizNum);
        sf.setFileId(fileId);
        CreateSignFlowRequest.NormalSignFieldConfig nfc = new CreateSignFlowRequest.NormalSignFieldConfig();
        nfc.setFreeMode(false);
        nfc.setAutoSign(false);
        nfc.setSignFieldStyle(1);
        CreateSignFlowRequest.SignFieldPosition pos = new CreateSignFlowRequest.SignFieldPosition();
        pos.setPositionPage("1");
        pos.setPositionX(posX);
        pos.setPositionY(posY);
        nfc.setSignFieldPosition(pos);
        sf.setNormalSignFieldConfig(nfc);
        s.setSignFields(List.of(sf));

        return s;
    }

    private Component comp(String componentId, String value) {
        Component c = new Component();
        c.setComponentId(componentId);
        c.setComponentValue(value != null ? value : "");
        return c;
    }

    private String fmtFen(java.math.BigDecimal amountFen) {
        if (amountFen == null) return "0";
        return amountFen.divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP).toString();
    }

    // ==================== 内嵌 DTO ====================

    // ----- 请求 -----
    @Data @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateFileRequest {
        private String docTemplateId;
        private String fileName;
        private boolean requiredCheck;
        private List<Component> components;
    }

    @Data @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Component {
        private String componentId;
        private String componentKey;
        private String componentValue;
    }

    @Data @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateSignFlowRequest {
        private List<Doc> docs;
        private SignFlowConfig signFlowConfig;
        private List<Signer> signers;

        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Doc { private String fileId; private String fileName; }
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class SignFlowConfig { private String signFlowTitle; private boolean autoFinish; }
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Signer {
            private SignConfig signConfig;
            private NoticeConfig noticeConfig;
            private int signerType;
            private PsnSignerInfo psnSignerInfo;
            private List<SignField> signFields;
        }
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class SignConfig { private int signOrder; }
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class NoticeConfig { private String noticeTypes; }
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class PsnSignerInfo {
            private String psnAccount;
            private PsnInfo psnInfo;
        }
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class PsnInfo {
            private String psnName;
            private String psnIDCardNum;
            private String psnIDCardType;
        }
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class SignField {
            private String customBizNum;
            private String fileId;
            private NormalSignFieldConfig normalSignFieldConfig;
        }
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class NormalSignFieldConfig {
            private boolean freeMode;
            private boolean autoSign;
            private int signFieldStyle;
            private SignFieldPosition signFieldPosition;
        }
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class SignFieldPosition {
            private String positionPage;
            private double positionX;
            private double positionY;
        }
    }

    @Data @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SignUrlRequest {
        private boolean needLogin;
        private int urlType;
        private Operator operator;
        @Data @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Operator { private String psnAccount; }
    }

    // ----- 响应（通用） -----
    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemplateDetailResponse {
        private int code; private String message;
        private TemplateDetailData data;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TemplateDetailData {
            private String docTemplateId; private String docTemplateName;
            private List<StructComponent> structComponents;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class StructComponent {
            private String componentId; private String componentKey;
            private String signerRole; private int componentType;
        }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateFileResponse {
        private int code; private String message;
        private CreateFileData data;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CreateFileData { private String fileId; private String fileDownloadUrl; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateSignFlowResponse {
        private int code; private String message;
        private SignFlowData data;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SignFlowData { private String signFlowId; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignUrlResponse {
        private int code; private String message;
        private SignUrlData data;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SignUrlData { private String url; private String shortUrl; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignFlowDetailResponse {
        private int code; private String message;
        private SignFlowDetailData data;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SignFlowDetailData {
            private String signFlowId; private int signFlowStatus;
            private String contractNum; private Long signFlowFinishTime;
            private List<SignerDetail> signers;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SignerDetail {
            private String signerRole;  // "甲方" / "乙方"
            private int signStatus;     // 0=未签, 1=已签
            private String psnAccount;
        }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileDownloadResponse {
        private int code; private String message;
        private FileDownloadData data;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FileDownloadData {
            private List<FileItem> files;
            private String certificateDownloadUrl;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FileItem {
            private String fileId; private String fileName; private String downloadUrl;
        }
    }
}
