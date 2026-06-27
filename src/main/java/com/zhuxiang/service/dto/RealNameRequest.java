package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "订单实名认证请求")
public record RealNameRequest(
        @NotBlank(message = "租客姓名不能为空")
        @Size(min = 1, max = 30, message = "姓名长度应为 1-30 位")
        @Schema(description = "租客真实姓名", example = "张三") String tenantName,

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式错误")
        @Schema(description = "租客联系电话", example = "13800138000") String tenantPhone,

        @NotBlank(message = "身份证号不能为空")
        @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式错误")
        @Schema(description = "18 位居民身份证号", example = "510100199001011234") String tenantIdCard,

        @NotBlank(message = "身份证人像面图片不能为空")
        @Schema(description = "身份证人像面图片 URL", example = "/api/uploads/id-card-front.jpg") String idCardFrontUrl,

        @NotBlank(message = "身份证国徽面图片不能为空")
        @Schema(description = "身份证国徽面图片 URL", example = "/api/uploads/id-card-back.jpg") String idCardBackUrl
) {
}
