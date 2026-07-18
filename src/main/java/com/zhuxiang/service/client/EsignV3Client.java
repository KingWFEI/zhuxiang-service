package com.zhuxiang.service.client;

import com.fasterxml.jackson.annotation.JsonAlias;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * e签宝 V3 电子合同客户端。
 * 签名复用 {@link EsignRequestSigner}，Body 只序列化一次。
 */
@Component
public class EsignV3Client {

    private static final Logger log = LoggerFactory.getLogger(EsignV3Client.class);
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

    public FileUploadResult uploadLocalFile(byte[] fileBytes, String fileName, String contentType) {
        requireConfigured();
        String md5 = signer.computeContentMd5(fileBytes);
        FileUploadUrlRequest request = new FileUploadUrlRequest();
        request.setContentMd5(md5);
        request.setContentType(contentType);
        request.setFileName(fileName);
        request.setFileSize(fileBytes.length);
        request.setConvertToPDF(false);
        String path = "/v3/files/file-upload-url";
        FileUploadUrlResponse response = post(path, request, FileUploadUrlResponse.class);
        ensureSuccess(response.getCode(), response.getMessage(), path);
        if (response.getData() == null) throw BusinessException.badRequest("e签宝未返回文件上传地址");
        try {
            restTemplate.execute(URI.create(response.getData().getFileUploadUrl()), HttpMethod.PUT,
                    clientRequest -> {
                        clientRequest.getHeaders().setContentType(MediaType.parseMediaType(contentType));
                        clientRequest.getHeaders().set("Content-MD5", md5);
                        clientRequest.getBody().write(fileBytes);
                    }, ignored -> null);
        } catch (RestClientException e) {
            throw new EsignException(0, "NETWORK", "合同源文件上传到e签宝失败", path);
        }
        return new FileUploadResult(response.getData().getFileId(), response.getData().getFileUploadUrl());
    }

    public FileStatusResponse getFileStatus(String fileId) {
        requireConfigured();
        String path = "/v3/files/" + fileId;
        FileStatusResponse response = get(path, FileStatusResponse.class);
        ensureSuccess(response.getCode(), response.getMessage(), path);
        return response;
    }

    public TemplatePageResponse getCreateTemplateUrl(String fileId, String templateName) {
        requireConfigured();
        TemplateCreateUrlRequest request = new TemplateCreateUrlRequest();
        request.setFileId(fileId);
        request.setDocTemplateName(templateName);
        request.setDocTemplateType(0);
        request.setSignerRoles(List.of("甲方", "乙方"));
        String path = "/v3/doc-templates/doc-template-create-url";
        TemplatePageResponse response = post(path, request, TemplatePageResponse.class);
        ensureSuccess(response.getCode(), response.getMessage(), path);
        return response;
    }

    public TemplatePageResponse getEditTemplateUrl(String docTemplateId) {
        requireConfigured();
        String path = "/v3/doc-templates/" + docTemplateId + "/doc-template-edit-url";
        TemplatePageResponse response = post(path, new TemplateEditUrlRequest(), TemplatePageResponse.class);
        ensureSuccess(response.getCode(), response.getMessage(), path);
        return response;
    }

    // ==================== 接口二：填写模板生成合同 ====================

    public CreateFileResponse createByDocTemplateComponents(List<Component> components,
                                                             String docTemplateId, String fileName) {
        requireConfigured();
        CreateFileRequest req = new CreateFileRequest();
        req.setDocTemplateId(docTemplateId);
        req.setFileName(fileName);
        req.setRequiredCheck(true);
        req.setComponents(components);
        String path = "/v3/files/create-by-doc-template";
        CreateFileResponse response = post(path, req, CreateFileResponse.class);
        ensureSuccess(response.getCode(), response.getMessage(), path);
        return response;
    }

    public Component keyedComponent(String componentKey, String value) {
        Component component = new Component();
        component.setComponentKey(componentKey);
        component.setComponentValue(value == null ? "" : value);
        return component;
    }

    // ==================== 接口三：发起签署流程 ====================

    public CreateSignFlowResponse createSignFlow(String contractFileId, LeaseContractFillData fillData,
                                                  int lessorPage, double lessorX, double lessorY,
                                                  int tenantPage, double tenantX, double tenantY) {
        requireConfigured();
        CreateSignFlowRequest req = buildSignFlowRequest(contractFileId, fillData,
                lessorPage, lessorX, lessorY, tenantPage, tenantX, tenantY);
        String path = "/v3/sign-flow/create-by-file";
        CreateSignFlowResponse response = post(path, req, CreateSignFlowResponse.class);
        ensureSuccess(response.getCode(), response.getMessage(), path);
        return response;
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
        if (!properties.isCredentialsConfigured()) {
            throw BusinessException.badRequest(
                    "e签宝电子合同未配置：请设置 ESIGN_APP_ID 和 ESIGN_APP_SECRET 环境变量");
        }
    }

    private String extractResponseBody(ClientHttpResponse response) throws java.io.IOException {
        byte[] bytes = response.getBody().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void ensureSuccess(int code, String message, String path) {
        if (code != 0) {
            throw EsignException.signingFailed(String.valueOf(code), message, path);
        }
    }

    private CreateSignFlowRequest buildSignFlowRequest(String contractFileId, LeaseContractFillData d,
                                                        int lessorPage, double lessorX, double lessorY,
                                                        int tenantPage, double tenantX, double tenantY) {
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
                "lessor_sign_001", contractFileId, lessorPage, lessorX, lessorY);
        // 乙方签署人
        CreateSignFlowRequest.Signer tenant = buildSigner(d.getTenantName(), d.getTenantMobile(), d.getTenantIdCard(),
                "tenant_sign_001", contractFileId, tenantPage, tenantX, tenantY);

        req.setSigners(List.of(lessor, tenant));
        return req;
    }

    private CreateSignFlowRequest.Signer buildSigner(String name, String mobile, String idCard,
                                                      String customBizNum, String fileId,
                                                      int page, double posX, double posY) {
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
        pos.setPositionPage(String.valueOf(page));
        pos.setPositionX(posX);
        pos.setPositionY(posY);
        nfc.setSignFieldPosition(pos);
        sf.setNormalSignFieldConfig(nfc);
        s.setSignFields(List.of(sf));

        return s;
    }

    // ==================== 内嵌 DTO ====================

    // ----- 请求 -----
    @Data @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileUploadUrlRequest {
        private String contentMd5;
        private String contentType;
        private String fileName;
        private long fileSize;
        private boolean convertToPDF;
    }

    @Data @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TemplateCreateUrlRequest {
        private String fileId;
        private String docTemplateName;
        private Integer docTemplateType;
        private List<String> signerRoles;
    }

    @Data @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TemplateEditUrlRequest {}

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
            private Long createTime; private Long updateTime;
            private String fileDownloadUrl;
            @JsonAlias("structComponents")
            private List<StructComponent> components;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class StructComponent {
            private String componentId; private String componentKey;
            private String componentName; private boolean required; private int componentType;
            private ComponentPosition componentPosition;
            private ComponentSize componentSize;
            private ComponentSpecialAttribute componentSpecialAttribute;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ComponentPosition {
            @JsonAlias({"page", "positionPage", "componentPageNum"}) private Integer pageNum;
            @JsonAlias({"positionX", "componentPositionX"}) private java.math.BigDecimal x;
            @JsonAlias({"positionY", "componentPositionY"}) private java.math.BigDecimal y;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ComponentSize {
            @JsonAlias("componentWidth") private java.math.BigDecimal width;
            @JsonAlias("componentHeight") private java.math.BigDecimal height;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ComponentSpecialAttribute {
            private String signerRole;
            private String dateFormat;
            private String numberFormat;
            private Integer componentMaxLength;
            private Integer componentMaxRows;
            private String componentAssociatedId;
        }
    }

    public record FileUploadResult(String fileId, String uploadUrl) {}

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileUploadUrlResponse {
        private int code; private String message; private FileUploadUrlData data;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FileUploadUrlData { private String fileId; private String fileUploadUrl; }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileStatusResponse {
        private int code; private String message; private FileStatusData data;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FileStatusData {
            private String fileId; private Integer fileStatus; private String fileDownloadUrl;
        }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemplatePageResponse {
        private int code; private String message; private TemplatePageData data;
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TemplatePageData {
            private String docTemplateId;
            @JsonAlias({"docTemplateCreateUrl", "docTemplateEditUrl", "url"}) private String url;
            @JsonAlias({"docTemplateCreateLongUrl", "docTemplateEditLongUrl", "longUrl"}) private String longUrl;
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
