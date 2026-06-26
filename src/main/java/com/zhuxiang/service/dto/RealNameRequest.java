package com.zhuxiang.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RealNameRequest(
        @NotBlank(message = "租客姓名不能为空")
        @Size(min = 1, max = 30, message = "姓名长度应为 1-30 位") String tenantName,

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式错误") String tenantPhone,

        @NotBlank(message = "身份证号不能为空")
        @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式错误") String tenantIdCard,

        @NotBlank(message = "身份证人像面图片不能为空") String idCardFrontUrl,

        @NotBlank(message = "身份证国徽面图片不能为空") String idCardBackUrl
) {
}
