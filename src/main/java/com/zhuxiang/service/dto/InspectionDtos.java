package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退租验收相关 DTO。
 */
public final class InspectionDtos {

    private InspectionDtos() {
    }

    // ==================== 模板相关 ====================

    @Schema(description = "验收项配置")
    public record TemplateCheckItem(
            @Schema(description = "设施/验收项编码", example = "wall") @NotBlank String itemCode,
            @Schema(description = "设施/验收项名称", example = "墙面") @NotBlank String itemName,
            @Schema(description = "是否必拍", example = "true") boolean required,
            @Schema(description = "最少照片数量", example = "2") int minPhotoCount,
            @Schema(description = "是否需要填写备注", example = "false") boolean remarkRequired,
            @Schema(description = "验收说明", example = "拍摄墙面整体及明显瑕疵") String instruction,
            @Schema(description = "是否启用", example = "true") boolean enabled
    ) {
    }

    @Schema(description = "房间验收配置")
    public record TemplateRoomItem(
            @Schema(description = "房间编码", example = "living_room") @NotBlank String roomCode,
            @Schema(description = "房间名称", example = "客厅") @NotBlank String roomName,
            @Schema(description = "该房间下的验收项列表") @Valid @NotNull List<TemplateCheckItem> items
    ) {
    }

    @Schema(description = "验收模板响应")
    public record TemplateResponse(
            @Schema(description = "房源ID") String houseId,
            @Schema(description = "模板版本号") Integer version,
            @Schema(description = "房间验收标准列表") List<TemplateRoomItem> rooms,
            @Schema(description = "最后更新时间") LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "保存验收模板请求")
    public record SaveTemplateRequest(
            @Schema(description = "房间验收标准列表") @Valid @NotNull List<TemplateRoomItem> rooms
    ) {
    }

    // ==================== 照片关联 ====================

    @Schema(description = "验收照片关联信息")
    public record PhotoItem(
            @Schema(description = "房间编码", example = "living_room") @NotBlank String roomCode,
            @Schema(description = "设施/验收项编码", example = "wall") @NotBlank String itemCode,
            @Schema(description = "照片URL") @NotBlank String url,
            @Schema(description = "拍摄时间") LocalDateTime capturedAt
    ) {
    }

    // ==================== 入住验收 ====================

    @Schema(description = "入住验收数据响应")
    public record MoveInInspectionResponse(
            @Schema(description = "快照ID") String snapshotId,
            @Schema(description = "合同ID") String contractId,
            @Schema(description = "房源ID") String houseId,
            @Schema(description = "模板版本号") Integer templateVersion,
            @Schema(description = "验收状态") String status,
            @Schema(description = "房间验收标准列表（含必拍提示）") List<TemplateRoomItem> rooms,
            @Schema(description = "已上传的入住照片，按 itemCode 分组") List<PhotoItem> existingPhotos
    ) {
    }

    @Schema(description = "提交入住验收请求")
    public record SubmitMoveInRequest(
            @Schema(description = "验收照片列表") @NotNull List<PhotoItem> photos
    ) {
    }

    // ==================== 退租验收 ====================

    @Schema(description = "退租验收数据响应")
    public record MoveOutInspectionResponse(
            @Schema(description = "快照ID") String snapshotId,
            @Schema(description = "合同ID") String contractId,
            @Schema(description = "房源ID") String houseId,
            @Schema(description = "验收状态") String status,
            @Schema(description = "房间验收标准列表（源自入住快照）") List<TemplateRoomItem> rooms,
            @Schema(description = "入住基准照片") List<PhotoItem> moveInPhotos,
            @Schema(description = "已上传的退租照片") List<PhotoItem> existingMoveOutPhotos
    ) {
    }

    @Schema(description = "提交退租验收请求")
    public record SubmitMoveOutRequest(
            @Schema(description = "验收照片列表") @NotNull List<PhotoItem> photos
    ) {
    }

    // ==================== 管理端对比 ====================

    @Schema(description = "验收对比项")
    public record ComparisonItem(
            @Schema(description = "设施/验收项编码") String itemCode,
            @Schema(description = "设施/验收项名称") String itemName,
            @Schema(description = "入住照片URL列表") List<String> moveInPhotos,
            @Schema(description = "退租照片URL列表") List<String> moveOutPhotos,
            @Schema(description = "对比结果") String result,
            @Schema(description = "扣款金额（分）") Integer deductionAmount,
            @Schema(description = "扣款原因") String reason,
            @Schema(description = "租客确认状态") String tenantStatus
    ) {
    }

    @Schema(description = "验收对比房间")
    public record ComparisonRoomItem(
            @Schema(description = "房间编码") String roomCode,
            @Schema(description = "房间名称") String roomName,
            @Schema(description = "验收对比项列表") List<ComparisonItem> items
    ) {
    }

    @Schema(description = "验收对比响应")
    public record ComparisonResponse(
            @Schema(description = "快照ID") String snapshotId,
            @Schema(description = "合同ID") String contractId,
            @Schema(description = "验收状态") String status,
            @Schema(description = "房间对比列表") List<ComparisonRoomItem> rooms,
            @Schema(description = "合计扣款金额（分）") Integer totalDeduction
    ) {
    }

    // ==================== 押金扣款 ====================

    @Schema(description = "扣款项请求")
    public record DeductionItemRequest(
            @Schema(description = "房间编码", example = "living_room") @NotBlank String roomCode,
            @Schema(description = "设施/验收项编码", example = "wall") @NotBlank String itemCode,
            @Schema(description = "对比结果", example = "NEW_DAMAGE") @NotBlank String result,
            @Schema(description = "扣款原因") String reason,
            @Schema(description = "扣款金额（分）") int deductionAmount,
            @Schema(description = "证据照片URL列表") List<String> evidenceUrls
    ) {
    }

    @Schema(description = "创建押金扣款请求")
    public record CreateSettlementRequest(
            @Schema(description = "扣款明细列表") @Valid @NotNull List<DeductionItemRequest> deductions
    ) {
    }

    @Schema(description = "扣款明细详情")
    public record DeductionItemDetail(
            @Schema(description = "扣款明细ID") String id,
            @Schema(description = "房间编码") String roomCode,
            @Schema(description = "设施/验收项编码") String itemCode,
            @Schema(description = "对比结果") String result,
            @Schema(description = "扣款原因") String reason,
            @Schema(description = "扣款金额（分）") Integer deductionAmount,
            @Schema(description = "证据照片URL列表") List<String> evidenceUrls,
            @Schema(description = "租客确认状态") String tenantStatus,
            @Schema(description = "租客异议原因") String tenantDisputeReason
    ) {
    }

    @Schema(description = "押金扣款结算响应")
    public record SettlementResponse(
            @Schema(description = "快照ID") String snapshotId,
            @Schema(description = "合计扣款金额（分）") Integer totalDeduction,
            @Schema(description = "扣款明细列表") List<DeductionItemDetail> deductions
    ) {
    }

    // ==================== 锁定验房 ====================

    @Schema(description = "锁定验房请求")
    public record LockRequest(
            @Schema(description = "验房完成备注", example = "2026-07-17 管家已完成现场验房，照片已归档") String comment
    ) {
    }
}
