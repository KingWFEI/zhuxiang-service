package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class LandlordDtos {

    private LandlordDtos() {
    }

    @Schema(description = "房东公开资料；本人查询时会额外返回未公开的联系方式")
    public record ProfileView(
            @Schema(description = "房东用户 ID") String userId,
            @Schema(description = "公开展示名称") String name,
            @Schema(description = "头像 URL") String avatarUrl,
            @Schema(description = "主页封面图 URL") String coverImageUrl,
            @Schema(description = "一句话个性签名") String slogan,
            @Schema(description = "个人介绍") String introduction,
            @Schema(description = "是否已实名认证") boolean isVerified,
            @Schema(description = "综合评分") BigDecimal rating,
            @Schema(description = "累计出租房源数量") Integer rentedCount,
            @Schema(description = "响应速度或服务说明") String responseDescription,
            @Schema(description = "主要服务区域") String serviceArea,
            @Schema(description = "从业或出租服务年限") Integer serviceYears,
            @Schema(description = "个性化服务标签") List<String> profileTags,
            @Schema(description = "联系电话；未公开时公共接口返回 null") String phone,
            @Schema(description = "微信号；未公开时公共接口返回 null") String wechat,
            @Schema(description = "联系邮箱；未公开时公共接口返回 null") String email,
            @Schema(description = "方便联系的时间说明") String contactTime,
            @Schema(description = "是否公开联系电话") boolean showPhone,
            @Schema(description = "是否公开微信号") boolean showWechat,
            @Schema(description = "是否公开联系邮箱") boolean showEmail,
            @Schema(description = "资料创建时间") LocalDateTime createdAt,
            @Schema(description = "资料最后更新时间") LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "房东维护自己的公开资料请求；未传字段保持不变，空字符串可清空可选文本")
    public record UpdateLandlordProfileRequest(
            @Size(min = 1, max = 50) @Schema(description = "公开展示名称") String name,
            @Size(max = 500) @Schema(description = "头像 URL") String avatarUrl,
            @Size(max = 500) @Schema(description = "主页封面图 URL") String coverImageUrl,
            @Size(max = 120) @Schema(description = "一句话个性签名") String slogan,
            @Size(max = 1000) @Schema(description = "个人介绍") String introduction,
            @Size(max = 200) @Schema(description = "主要服务区域") String serviceArea,
            @Min(0) @Max(80) @Schema(description = "从业或出租服务年限") Integer serviceYears,
            @Size(max = 8) @Schema(description = "个性化服务标签，最多 8 个") List<
                    @NotBlank @Size(max = 20) String> profileTags,
            @Size(max = 20) @Schema(description = "联系电话") String phone,
            @Size(max = 100) @Schema(description = "微信号") String wechat,
            @Email @Size(max = 150) @Schema(description = "联系邮箱") String email,
            @Size(max = 100) @Schema(description = "方便联系的时间说明") String contactTime,
            @Size(max = 100) @Schema(description = "响应速度或服务说明") String responseDescription,
            @Schema(description = "是否向租客公开联系电话") Boolean showPhone,
            @Schema(description = "是否向租客公开微信号") Boolean showWechat,
            @Schema(description = "是否向租客公开联系邮箱") Boolean showEmail
    ) {
    }
}
