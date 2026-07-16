package com.zhuxiang.service.config;

import com.zhuxiang.service.client.EsignV3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 应用启动时校验 e签宝合同模板控件是否与代码中硬编码的 componentId 一致。
 * <p>
 * 仅在 e签宝正确配置时执行；配置缺失或网络不可用时只打 WARN 不阻塞启动。
 */
@Component
public class EsignTemplateValidator {

    private static final Logger log = LoggerFactory.getLogger(EsignTemplateValidator.class);

    /** 代码中使用的控件 componentId 集合 */
    private static final Set<String> EXPECTED_COMPONENT_IDS = Set.of(
            "5a370264c031478881f3190bc57ed0d1", // 甲方手机号
            "4d9af6b5635a4ac1b3cac118dbdfc158", // 乙方手机号
            "3f2828bb27ce428389b7705d8cee4583", // 甲方签字日期
            "8db2c90bd40848f882e6a11e9e5b8ffb", // 乙方签字日期
            "4b7411bae10f4245bd3dcc8966d8aa96", // 甲方姓名
            "df752b99852c4dafb55053ed8dbccc4d", // 乙方姓名
            "6076621c6ff841f9ba7785b7f2953c07", // 甲方身份证号
            "b02f5f912f2444eaa049ea62e02c5304", // 乙方身份证号
            "18a756d26a24414394f07d0e0edfa7eb", // 房屋坐落地点
            "088601e5dbb44977865b79d172d585c8", // 房屋租期（年）
            "51841a2427d54eacaf98c129025ad8f0", // 起租日期
            "28a5bc5ef0694366a264816bb196fb18", // 租期截至日期
            "c6830e38f7c14311aa0231c47f03f6cb", // 不续租提前告知日期
            "2cf9ab0ea4e24451b0758fce1e43809f", // 押金
            "74fea2206ae446dab401d4ad8782b89a", // 租金
            "51a4799042384cb095728c8f38e76a6f"  // 租金交付日
    );

    /** 签名控件 componentId：甲方 */
    private static final String LESSOR_SIGN_ID = "ea7fbed7e9994185866a37d77c399cec";
    /** 签名控件 componentId：乙方 */
    private static final String TENANT_SIGN_ID = "105deee6c3de4349bb12d6d9dddffc10";

    private final EsignV3Client esignV3Client;
    private final EsignV3Properties properties;

    public EsignTemplateValidator(EsignV3Client esignV3Client, EsignV3Properties properties) {
        this.esignV3Client = esignV3Client;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (!properties.isConfigured()) {
            log.info("e签宝未配置，跳过合同模板校验");
            return;
        }

        String templateId = properties.getDocTemplateId();
        if (templateId == null || templateId.isBlank()) {
            log.warn("e签宝 doc-template-id 未配置，跳过合同模板校验");
            return;
        }

        try {
            EsignV3Client.TemplateDetailResponse resp = esignV3Client.getTemplateDetail(templateId);
            if (resp.getData() == null || resp.getData().getStructComponents() == null) {
                log.warn("e签宝模板 {} 不存在或无控件数据，请联系管理员检查", templateId);
                return;
            }

            List<EsignV3Client.TemplateDetailResponse.StructComponent> components =
                    resp.getData().getStructComponents();

            // 校验：16 个普通填充控件是否存在
            Set<String> templateComponentIds = components.stream()
                    .filter(c -> c.getComponentId() != null)
                    .map(EsignV3Client.TemplateDetailResponse.StructComponent::getComponentId)
                    .collect(java.util.stream.Collectors.toSet());

            for (String expected : EXPECTED_COMPONENT_IDS) {
                if (!templateComponentIds.contains(expected)) {
                    log.error("【模板异常】e签宝模板 {} 缺少控件 componentId={}，请联系管理员更新模板", templateId, expected);
                }
            }

            // 校验：甲方签名区 (signerRole = 甲方)
            boolean lessorSignOk = components.stream().anyMatch(c ->
                    LESSOR_SIGN_ID.equals(c.getComponentId()) && "甲方".equals(c.getSignerRole()));
            if (!lessorSignOk) {
                log.error("【模板异常】e签宝模板 {} 甲方签名区缺失或 signerRole 不是'甲方': componentId={}",
                        templateId, LESSOR_SIGN_ID);
            }

            // 校验：乙方签名区 (signerRole = 乙方)
            boolean tenantSignOk = components.stream().anyMatch(c ->
                    TENANT_SIGN_ID.equals(c.getComponentId()) && "乙方".equals(c.getSignerRole()));
            if (!tenantSignOk) {
                log.error("【模板异常】e签宝模板 {} 乙方签名区缺失或 signerRole 不是'乙方': componentId={}",
                        templateId, TENANT_SIGN_ID);
            }

            // 统计校验结果
            long missingCount = EXPECTED_COMPONENT_IDS.stream()
                    .filter(id -> !templateComponentIds.contains(id)).count();
            if (missingCount == 0 && lessorSignOk && tenantSignOk) {
                log.info("e签宝模板 {} 校验通过：{} 个控件完整，甲乙双方签名区正常",
                        templateId, EXPECTED_COMPONENT_IDS.size());
            } else {
                log.warn("e签宝模板 {} 校验完成：{} 个控件缺失，甲方签名区{}，乙方签名区{}",
                        templateId, missingCount,
                        lessorSignOk ? "正常" : "异常",
                        tenantSignOk ? "正常" : "异常");
            }
        } catch (Exception e) {
            log.warn("e签宝模板校验失败（不影响业务启动）: templateId={}, error={}",
                    templateId, e.getMessage());
        }
    }
}
