package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.LandlordContractDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.LeaseTerminationApplication;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.LeaseTerminationApplicationMapper;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LandlordTerminationService;
import com.zhuxiang.service.service.LeaseTerminationService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class LandlordTerminationServiceImpl implements LandlordTerminationService {
    private static final Map<String, String> STATUS_TEXT = Map.of(
            "rescission_pending", "待发起解约协议",
            "rescission_signing", "解约协议待签署",
            "rescission_failed", "合同解约异常",
            "completed", "解约已完成"
    );

    private final LeaseTerminationApplicationMapper applicationMapper;
    private final RentContractMapper contractMapper;
    private final RentOrderMapper orderMapper;
    private final HouseService houseService;
    private final UserService userService;
    private final EsignV3Client esignClient;
    private final LeaseTerminationService terminationService;

    public LandlordTerminationServiceImpl(
            LeaseTerminationApplicationMapper applicationMapper,
            RentContractMapper contractMapper,
            RentOrderMapper orderMapper,
            HouseService houseService,
            UserService userService,
            EsignV3Client esignClient,
            LeaseTerminationService terminationService) {
        this.applicationMapper = applicationMapper;
        this.contractMapper = contractMapper;
        this.orderMapper = orderMapper;
        this.houseService = houseService;
        this.userService = userService;
        this.esignClient = esignClient;
        this.terminationService = terminationService;
    }

    @Override
    public PageData<LandlordContractDtos.TerminationItem> listPendingSign(
            String landlordUserId, long page, long pageSize) {
        requireLandlord(landlordUserId);
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw BusinessException.badRequest("分页参数不正确");
        }
        long total = applicationMapper.countLandlordPendingRescissions(landlordUserId);
        List<LandlordContractDtos.TerminationItem> items = applicationMapper
                .selectLandlordPendingRescissions(landlordUserId, (page - 1) * pageSize, pageSize)
                .stream().map(this::toItem).toList();
        return PageData.of(items, page, pageSize, total);
    }

    @Override
    public LandlordContractDtos.TerminationDetail getDetail(
            String landlordUserId, String applicationId) {
        LeaseTerminationApplication app = requireOwnedApplication(landlordUserId, applicationId);
        return new LandlordContractDtos.TerminationDetail(
                toItem(app), app.getReason(), app.getActualMoveOutDate(),
                value(app.getRefundAmount()), value(app.getTotalDeduction()));
    }

    @Override
    public LandlordContractDtos.RescissionAction getSignUrl(
            String landlordUserId, String applicationId) {
        LeaseTerminationApplication app = requireOwnedApplication(landlordUserId, applicationId);
        if (LeaseTerminationApplication.STATUS_RESCISSION_PENDING.equals(app.getStatus())) {
            terminationService.processPendingFlow(applicationId);
            app = requireOwnedApplication(landlordUserId, applicationId);
        }
        if (!LeaseTerminationApplication.STATUS_RESCISSION_SIGNING.equals(app.getStatus())
                || !StringUtils.hasText(app.getRescissionSignFlowId())) {
            throw BusinessException.badRequest(StringUtils.hasText(app.getProcessLastError())
                    ? app.getProcessLastError() : "解约协议尚未进入签署阶段");
        }
        RentContract contract = requireContract(app.getContractId());
        if (!StringUtils.hasText(contract.getLandlordPhone())) {
            throw BusinessException.badRequest("房东缺少签署手机号");
        }
        EsignV3Client.SignUrlResponse response = esignClient.getRescissionSignUrl(
                app.getRescissionSignFlowId(), contract.getLandlordPhone().trim());
        if (response.getData() == null || !StringUtils.hasText(response.getData().getUrl())) {
            throw new IllegalStateException("e签宝未返回房东解约协议签署链接");
        }
        SignatureState state = signatureState(app, contract.getLandlordPhone());
        return new LandlordContractDtos.RescissionAction(
                "sign", app.getRescissionSignFlowId(), response.getData().getUrl(),
                response.getData().getShortUrl(), app.getStatus(), state.currentUserSigned(), state.completed());
    }

    @Override
    public LandlordContractDtos.RescissionAction refresh(
            String landlordUserId, String applicationId) {
        LeaseTerminationApplication app = requireOwnedApplication(landlordUserId, applicationId);
        if (LeaseTerminationApplication.STATUS_RESCISSION_SIGNING.equals(app.getStatus())) {
            terminationService.processPendingFlow(applicationId);
            app = requireOwnedApplication(landlordUserId, applicationId);
        }
        RentContract contract = requireContract(app.getContractId());
        SignatureState state = signatureState(app, contract.getLandlordPhone());
        return new LandlordContractDtos.RescissionAction(
                "status", app.getRescissionSignFlowId(), null, null,
                app.getStatus(), state.currentUserSigned(), state.completed());
    }

    private SignatureState signatureState(LeaseTerminationApplication app, String landlordPhone) {
        if (LeaseTerminationApplication.STATUS_COMPLETED.equals(app.getStatus())) {
            return new SignatureState(true, true);
        }
        if (!StringUtils.hasText(app.getRescissionSignFlowId())) return new SignatureState(false, false);
        EsignV3Client.SignFlowDetailResponse detail = esignClient.getSignFlowDetail(app.getRescissionSignFlowId());
        boolean completed = detail.getData() != null && detail.getData().getSignFlowStatus() == 2;
        boolean signed = detail.getData() != null && detail.getData().getSigners() != null
                && detail.getData().getSigners().stream().anyMatch(signer ->
                landlordPhone != null && landlordPhone.equals(signer.resolvedPsnAccount())
                        && signer.getSignStatus() == 2);
        return new SignatureState(signed, completed);
    }

    private LandlordContractDtos.TerminationItem toItem(LeaseTerminationApplication app) {
        RentContract contract = requireContract(app.getContractId());
        House house = houseService.getById(app.getHouseId());
        SignatureState state = StringUtils.hasText(app.getRescissionSignFlowId())
                ? signatureState(app, contract.getLandlordPhone()) : new SignatureState(false, false);
        boolean tenantSigned = false;
        if (StringUtils.hasText(app.getRescissionSignFlowId()) && StringUtils.hasText(contract.getTenantPhone())) {
            EsignV3Client.SignFlowDetailResponse detail = esignClient.getSignFlowDetail(app.getRescissionSignFlowId());
            tenantSigned = detail.getData() != null && detail.getData().getSigners() != null
                    && detail.getData().getSigners().stream().anyMatch(signer ->
                    contract.getTenantPhone().equals(signer.resolvedPsnAccount()) && signer.getSignStatus() == 2);
        }
        return new LandlordContractDtos.TerminationItem(
                app.getId(), app.getApplicationNo(), app.getStatus(),
                STATUS_TEXT.getOrDefault(app.getStatus(), app.getStatus()),
                contract.getId(), contract.getContractNo(), contract.getHouseName(), contract.getRoomName(),
                contract.getHouseAddress() != null ? contract.getHouseAddress()
                        : house == null ? "" : house.getAddress(),
                contract.getTenantName(), maskPhone(contract.getTenantPhone()), app.getExpectedMoveOutDate(),
                tenantSigned, state.currentUserSigned(), app.getUpdatedAt());
    }

    private LeaseTerminationApplication requireOwnedApplication(String landlordUserId, String applicationId) {
        requireLandlord(landlordUserId);
        LeaseTerminationApplication app = applicationMapper.selectById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        RentContract contract = requireContract(app.getContractId());
        RentOrder order = orderMapper.selectById(contract.getOrderId());
        if (order == null || !landlordUserId.equals(order.getLessorUserId())) {
            throw BusinessException.forbidden("无权查看该解约协议");
        }
        return app;
    }

    private RentContract requireContract(String contractId) {
        RentContract contract = contractMapper.selectById(contractId);
        if (contract == null) throw BusinessException.notFound("原租赁合同不存在");
        return contract;
    }

    private void requireLandlord(String userId) {
        User user = userService.getById(userId);
        if (user == null || !"LANDLORD".equalsIgnoreCase(user.getRole())) {
            throw BusinessException.forbidden("仅房东可以操作解约协议");
        }
    }

    private int value(Integer amount) { return amount == null ? 0 : amount; }
    private String maskPhone(String phone) {
        return phone == null || phone.length() < 7 ? "" : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    private record SignatureState(boolean currentUserSigned, boolean completed) {}
}
