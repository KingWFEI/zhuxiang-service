package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 个人实名认证相关 DTO。
 */
public final class RealNameAuthDtos {

    private RealNameAuthDtos() {
    }

    @Schema(description = "发起实名认证请求")
    public record StartRequest(
            @NotBlank(message = "姓名不能为空")
            @Schema(description = "真实姓名", example = "测试用户")
            String realName,

            @NotBlank(message = "证件类型不能为空")
            @Pattern(regexp = "INDIVIDUAL_CH_IDCARD", message = "当前仅支持 INDIVIDUAL_CH_IDCARD")
            @Schema(description = "证件类型", example = "INDIVIDUAL_CH_IDCARD")
            String idCardType,

            @NotBlank(message = "身份证号不能为空")
            @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式错误")
            @Schema(description = "18 位居民身份证号", example = "110101199001010000")
            String idCardNo
    ) {
    }

    @Schema(description = "重新发起实名认证请求（强制过期旧 VERIFYING 任务）")
    public record RestartRequest(
            @NotBlank(message = "姓名不能为空")
            @Schema(description = "真实姓名", example = "测试用户")
            String realName,

            @NotBlank(message = "证件类型不能为空")
            @Pattern(regexp = "INDIVIDUAL_CH_IDCARD", message = "当前仅支持 INDIVIDUAL_CH_IDCARD")
            @Schema(description = "证件类型", example = "INDIVIDUAL_CH_IDCARD")
            String idCardType,

            @NotBlank(message = "身份证号不能为空")
            @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式错误")
            @Schema(description = "18 位居民身份证号", example = "110101199001010000")
            String idCardNo
    ) {
    }

    @Schema(description = "发起实名认证响应")
    public record StartResult(
            @Schema(description = "平台实名认证业务流水号") String realNameAuthNo,
            @Schema(description = "认证状态") String authStatus,
            @Schema(description = "脱敏身份证号") String idCardMasked,
            @Schema(description = "e签宝H5认证地址") String authUrl,
            @Schema(description = "认证地址过期时间") String authUrlExpireTime
    ) {
    }

    @Schema(description = "刷新认证结果响应")
    public record RefreshResult(
            @Schema(description = "平台实名认证业务流水号") String realNameAuthNo,
            @Schema(description = "认证状态") String authStatus,
            @Schema(description = "脱敏姓名") String realNameMasked,
            @Schema(description = "脱敏身份证号") String idCardMasked,
            @Schema(description = "脱敏平台绑定手机号") String accountMobileMasked,
            @Schema(description = "脱敏已核验手机号") String verifiedMobileMasked,
            @Schema(description = "认证完成时间") String verifiedAt,
            @Schema(description = "e签宝H5认证地址，仅 VERIFYING 且未过期时返回") String authUrl,
            @Schema(description = "认证地址过期时间，仅 VERIFYING 且未过期时返回") String authUrlExpireTime
    ) {
    }

    @Schema(description = "当前用户实名认证状态")
    public record StatusResult(
            @Schema(description = "平台实名认证业务流水号") String realNameAuthNo,
            @Schema(description = "认证状态") String authStatus,
            @Schema(description = "脱敏姓名") String realNameMasked,
            @Schema(description = "脱敏身份证号") String idCardMasked,
            @Schema(description = "脱敏平台绑定手机号") String accountMobileMasked,
            @Schema(description = "脱敏已核验手机号") String verifiedMobileMasked,
            @Schema(description = "认证完成时间") String verifiedAt,
            @Schema(description = "认证地址过期时间") String authUrlExpireTime,
            @Schema(description = "e签宝H5认证地址，仅 VERIFYING 且未过期时返回") String authUrl
    ) {
    }
}
