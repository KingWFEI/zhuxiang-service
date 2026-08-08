package com.zhuxiang.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class LandlordAuthDtos {
    private LandlordAuthDtos() {
    }

    public record ProofRequest(
            @NotBlank String proofType,
            @NotBlank String fileId,
            @NotBlank String fileUrl
    ) {
    }

    public record SubmitRequest(
            @NotBlank @Size(max = 80) String realName,
            @NotBlank
            @Pattern(regexp = "^[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$", message = "身份证号格式不正确")
            String idCardNo,
            @NotBlank String idCardFrontUrl,
            @NotBlank String idCardBackUrl,
            @NotEmpty @Valid List<ProofRequest> proofs,
            @NotBlank
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
            String contactPhone,
            @Size(max = 100) String contactWechat,
            @Email @Size(max = 160) String contactEmail,
            @Size(max = 500) String contactAddress,
            @Size(max = 160) String preferredContactTime,
            @Size(max = 1000) String applicantNote,
            boolean replaceExisting
    ) {
    }

    public record ProofView(
            String id,
            String proofType,
            String fileId,
            String fileUrl
    ) {
    }

    public record ApplicationView(
            String id,
            String applicationNo,
            String userId,
            String status,
            String realName,
            String idCardMasked,
            String idCardFrontUrl,
            String idCardBackUrl,
            String contactPhone,
            String contactWechat,
            String contactEmail,
            String contactAddress,
            String preferredContactTime,
            String applicantNote,
            String rejectReason,
            String reviewerId,
            LocalDateTime reviewedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<ProofView> proofs
    ) {
    }

    public record StatusView(
            String status,
            boolean canSubmit,
            boolean landlord,
            ApplicationView latestApplication
    ) {
    }

    public record AdminListItem(
            String id,
            String applicationNo,
            String userId,
            String applicantName,
            String contactPhone,
            String userNickname,
            String userPhone,
            String status,
            int proofCount,
            LocalDateTime createdAt,
            LocalDateTime reviewedAt
    ) {
    }

    public record ReviewRequest(
            @NotBlank @Pattern(regexp = "APPROVED|REJECTED", message = "审核结果仅支持 APPROVED 或 REJECTED") String decision,
            @Size(max = 1000) String reason
    ) {
    }
}
