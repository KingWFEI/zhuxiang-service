package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.LeaseTerminationDtos;
import com.zhuxiang.service.entity.LeaseTerminationApplication;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LeaseTerminationService extends IService<LeaseTerminationApplication> {

    LeaseTerminationDtos.TerminationCheckResponse checkTermination(String userId, String leaseId);

    LeaseTerminationDtos.ApplyResponse apply(String userId, String leaseId, LeaseTerminationDtos.ApplyRequest request);

    LeaseTerminationDtos.TerminationDetailResponse getCurrent(String userId, String leaseId);

    LeaseTerminationDtos.TerminationDetailResponse getDetail(String userId, String applicationId);

    void supplement(String userId, String applicationId, LeaseTerminationDtos.SupplementRequest request);

    void cancel(String userId, String applicationId, LeaseTerminationDtos.CancelRequest request);

    LeaseTerminationDtos.TerminationDetailResponse getDetailForAdmin(String applicationId);

    LeaseTerminationDtos.TerminationDetailResponse approve(String adminId, String applicationId);

    LeaseTerminationDtos.TerminationDetailResponse reject(String adminId, String applicationId, LeaseTerminationDtos.RejectRequest request);

    LeaseTerminationDtos.TerminationDetailResponse requestSupplement(String adminId, String applicationId, LeaseTerminationDtos.SupplementReasonRequest request);

    LeaseTerminationDtos.TerminationDetailResponse completeInspection(String adminId, String applicationId);

    LeaseTerminationDtos.TerminationDetailResponse confirmSettlement(
            String adminId,
            String applicationId,
            LeaseTerminationDtos.SettlementConfirmRequest request
    );

    LeaseTerminationDtos.TerminationDetailResponse completeRefund(String adminId, String applicationId);
}
