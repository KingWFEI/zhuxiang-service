package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "退租申请")
public final class LeaseTerminationDtos {

    private LeaseTerminationDtos() {
    }

    @Schema(description = "校验退租条件响应")
    public record TerminationCheckResponse(
            @Schema(description = "是否可以申请退租") boolean canApply,
            @Schema(description = "是否存在进行中的退租申请") boolean hasProcessingApplication,
            @Schema(description = "欠费金额(分)") int unpaidAmount,
            @Schema(description = "押金金额(分)") int depositAmount,
            @Schema(description = "合同结束日期") LocalDate contractEndDate,
            @Schema(description = "提示信息") List<String> tips
    ) {
    }

    @Schema(description = "提交退租申请请求")
    public record ApplyRequest(
            @Schema(description = "退租原因") @NotBlank @Size(max = 500) String reason,
            @Schema(description = "期望退租日期") @NotNull LocalDate expectedMoveOutDate,
            @Schema(description = "是否已搬离") boolean hasMovedOut,
            @Schema(description = "联系人姓名") @NotBlank @Size(max = 100) String contactName,
            @Schema(description = "联系人手机号") @NotBlank @Size(max = 20) String contactPhone,
            @Schema(description = "备注") @Size(max = 1000) String remark,
            @Schema(description = "附件列表") @Valid List<AttachmentItem> attachments
    ) {
    }

    @Schema(description = "附件")
    public record AttachmentItem(
            @Schema(description = "文件URL") @NotBlank String url,
            @Schema(description = "文件类型: image/file") String type,
            @Schema(description = "文件名称") String name
    ) {
    }

    @Schema(description = "提交退租申请响应")
    public record ApplyResponse(
            @Schema(description = "退租申请ID") String id,
            @Schema(description = "退租申请编号") String applicationNo,
            @Schema(description = "状态") String status,
            @Schema(description = "状态文本") String statusText
    ) {
    }

    @Schema(description = "退租申请详情")
    public record TerminationDetailResponse(
            @Schema(description = "申请ID") String id,
            @Schema(description = "申请编号") String applicationNo,
            @Schema(description = "合同ID") String contractId,
            @Schema(description = "房源ID") String houseId,
            @Schema(description = "房源名称") String houseName,
            @Schema(description = "退租原因") String reason,
            @Schema(description = "期望退租日期") LocalDate expectedMoveOutDate,
            @Schema(description = "是否已搬离") Boolean hasMovedOut,
            @Schema(description = "联系人姓名") String contactName,
            @Schema(description = "联系人手机号") String contactPhone,
            @Schema(description = "备注") String remark,
            @Schema(description = "附件列表") List<AttachmentItem> attachments,
            @Schema(description = "状态") String status,
            @Schema(description = "状态文本") String statusText,
            @Schema(description = "驳回原因") String rejectReason,
            @Schema(description = "补充材料要求") String supplementReason,
            @Schema(description = "总扣款(分)") Integer totalDeduction,
            @Schema(description = "应退金额(分)") Integer refundAmount,
            @Schema(description = "实际搬离日期") LocalDate actualMoveOutDate,
            @Schema(description = "创建时间") LocalDateTime createdAt,
            @Schema(description = "更新时间") LocalDateTime updatedAt,
            @Schema(description = "时间线") List<TimelineItem> timeline
    ) {
    }

    @Schema(description = "时间线条目")
    public record TimelineItem(
            @Schema(description = "状态") String status,
            @Schema(description = "标题") String title,
            @Schema(description = "时间") LocalDateTime time
    ) {
    }

    @Schema(description = "补充材料请求")
    public record SupplementRequest(
            @Schema(description = "退租原因") @NotBlank @Size(max = 500) String reason,
            @Schema(description = "期望退租日期") @NotNull LocalDate expectedMoveOutDate,
            @Schema(description = "是否已搬离") boolean hasMovedOut,
            @Schema(description = "联系人姓名") @NotBlank @Size(max = 100) String contactName,
            @Schema(description = "联系人手机号") @NotBlank @Size(max = 20) String contactPhone,
            @Schema(description = "备注") @Size(max = 1000) String remark,
            @Schema(description = "附件列表") @Valid List<AttachmentItem> attachments
    ) {
    }

    @Schema(description = "撤销申请请求")
    public record CancelRequest(
            @Schema(description = "撤销原因") @NotBlank @Size(max = 1000) String cancelReason
    ) {
    }

    @Schema(description = "驳回请求")
    public record RejectRequest(
            @Schema(description = "驳回原因") @NotBlank @Size(max = 1000) String rejectReason
    ) {
    }

    @Schema(description = "要求补充材料请求")
    public record SupplementReasonRequest(
            @Schema(description = "补充材料要求") @NotBlank @Size(max = 1000) String supplementReason
    ) {
    }

    @Schema(description = "确认退租结算请求")
    public record SettlementConfirmRequest(
            @Schema(description = "结算扣款金额(分)") Integer settlementAmount,
            @Schema(description = "应退金额(分)") Integer refundAmount,
            @Schema(description = "结算备注") @Size(max = 1000) String remark
    ) {
    }
}
