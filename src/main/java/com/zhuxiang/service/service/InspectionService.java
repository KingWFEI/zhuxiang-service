package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.InspectionDtos;
import com.zhuxiang.service.entity.LeaseInspectionSnapshot;

/**
 * 租约验收服务：快照、入住/退租验收、对比与押金扣款。
 */
public interface InspectionService extends IService<LeaseInspectionSnapshot> {

    /** 签约完成时从房源模板复制验收标准到租约快照。不存在模板时静默跳过。 */
    void createSnapshotFromTemplate(String contractId, String leaseId, String houseId);

    // ---------- 入住验收（App 端） ----------

    InspectionDtos.MoveInInspectionResponse getMoveInInspection(String userId, String contractId);

    void submitMoveInInspection(String userId, String contractId, InspectionDtos.SubmitMoveInRequest request);

    /** 租客确认入住验收 */
    void confirmMoveIn(String userId, String contractId);

    // ---------- 退租验收（App 端） ----------

    InspectionDtos.MoveOutInspectionResponse getMoveOutInspection(String userId, String contractId);

    void submitMoveOutInspection(String userId, String contractId, InspectionDtos.SubmitMoveOutRequest request);

    // ---------- 管理端对比与扣款 ----------

    InspectionDtos.ComparisonResponse getComparison(String contractId);

    InspectionDtos.SettlementResponse createSettlement(String adminId, String contractId, InspectionDtos.CreateSettlementRequest request);

    // ---------- 押金结算查询与确认 ----------

    /** 租客查看押金扣款明细 */
    InspectionDtos.SettlementResponse getTenantSettlement(String userId, String contractId);

    /** 管理端查看押金扣款明细 */
    InspectionDtos.SettlementResponse getAdminSettlement(String contractId);

    // ---------- 管理端锁定验房 ----------

    /** 管理端确认线下验房完成并锁定照片证据（SUBMITTED → LOCKED），记录完成人和备注，幂等。 */
    void lockInspection(String adminId, String contractId, InspectionDtos.LockRequest request);
}
