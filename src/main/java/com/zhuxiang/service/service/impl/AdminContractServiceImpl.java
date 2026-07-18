package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminContractDtos;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.service.AdminContractService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminContractServiceImpl implements AdminContractService {
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "HOUSEKEEPER");
    private static final Set<String> STATUSES = Set.of(
            "draft", "confirmed", "generated", "signing", "signed", "canceled", "expired", "terminated", "failed");

    private final RentContractMapper contractMapper;
    private final UserService userService;
    private final EsignV3Client esignClient;

    public AdminContractServiceImpl(RentContractMapper contractMapper, UserService userService,
                                    EsignV3Client esignClient) {
        this.contractMapper = contractMapper;
        this.userService = userService;
        this.esignClient = esignClient;
    }

    @Override
    public PageData<AdminContractDtos.ContractSummary> list(String operatorId, String status, String keyword,
                                                            long page, long pageSize) {
        requireRole(operatorId);
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toLowerCase(Locale.ROOT) : null;
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) {
            throw BusinessException.badRequest("不支持的合同状态");
        }
        String q = StringUtils.hasText(keyword) ? keyword.trim() : null;
        LambdaQueryWrapper<RentContract> wrapper = new LambdaQueryWrapper<RentContract>()
                .eq(normalizedStatus != null, RentContract::getStatus, normalizedStatus)
                .and(q != null, w -> w.like(RentContract::getContractNo, q)
                        .or().like(RentContract::getContractNum, q)
                        .or().like(RentContract::getOrderId, q)
                        .or().like(RentContract::getTenantName, q)
                        .or().like(RentContract::getTenantPhone, q)
                        .or().like(RentContract::getLandlordName, q)
                        .or().like(RentContract::getLandlordPhone, q)
                        .or().like(RentContract::getHouseName, q)
                        .or().like(RentContract::getHouseAddress, q))
                .orderByDesc(RentContract::getCreatedAt);
        Page<RentContract> result = contractMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageData.of(result.getRecords().stream().map(this::summary).toList(),
                page, pageSize, result.getTotal());
    }

    @Override
    public AdminContractDtos.ContractDetail get(String operatorId, String contractId) {
        requireRole(operatorId);
        return detail(requireContract(contractId));
    }

    @Override
    public AdminContractDtos.DownloadUrl downloadUrl(String operatorId, String contractId) {
        requireRole(operatorId);
        RentContract contract = requireContract(contractId);
        if (!StringUtils.hasText(contract.getContractFileId())) throw BusinessException.badRequest("合同文件尚未生成");
        String url = null;
        String fileName = (StringUtils.hasText(contract.getContractNo()) ? contract.getContractNo() : "租房合同") + ".pdf";
        if (StringUtils.hasText(contract.getSignFlowId()) && "signed".equals(contract.getStatus())) {
            EsignV3Client.FileDownloadResponse response = esignClient.getFileDownloadUrl(contract.getSignFlowId());
            if (response.getData() != null && response.getData().getFiles() != null && !response.getData().getFiles().isEmpty()) {
                var file = response.getData().getFiles().get(0);
                url = file.getDownloadUrl();
                if (StringUtils.hasText(file.getFileName())) fileName = file.getFileName();
            }
        } else {
            EsignV3Client.FileStatusResponse response = esignClient.getFileStatus(contract.getContractFileId());
            if (response.getData() != null) url = response.getData().getFileDownloadUrl();
        }
        if (!StringUtils.hasText(url)) throw BusinessException.badRequest("暂时无法获取合同文件地址");
        return new AdminContractDtos.DownloadUrl(fileName, url, LocalDateTime.now().plusHours(1));
    }

    private RentContract requireContract(String id) {
        RentContract contract = contractMapper.selectById(id);
        if (contract == null) throw BusinessException.notFound("合同不存在");
        return contract;
    }

    private void requireRole(String operatorId) {
        User user = userService.requireActiveUser(operatorId);
        if (!ADMIN_ROLES.contains(user.getRole())) throw BusinessException.forbidden("当前账号无权查看合同");
    }

    private AdminContractDtos.ContractSummary summary(RentContract c) {
        return new AdminContractDtos.ContractSummary(c.getId(), c.getContractNo(), c.getContractNum(), c.getOrderId(),
                c.getHouseId(), c.getHouseName(), c.getHouseAddress(), c.getTenantName(), c.getTenantPhone(),
                c.getLandlordName(), c.getLandlordPhone(), c.getStatus(), signed(c.getLessorSigned()),
                signed(c.getTenantSigned()), c.getStartDate(), c.getEndDate(), c.getLeaseMonths(),
                c.getMonthlyRent(), c.getDeposit(), c.getTemplateVersion(), StringUtils.hasText(c.getContractFileId()),
                c.getSignedAt(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private AdminContractDtos.ContractDetail detail(RentContract c) {
        return new AdminContractDtos.ContractDetail(c.getId(), c.getContractNo(), c.getContractNum(), c.getOrderId(),
                c.getUserId(), c.getHouseId(), c.getHouseName(), c.getRoomName(), c.getHouseAddress(),
                c.getTenantName(), c.getTenantPhone(), c.getLandlordName(), c.getLandlordPhone(), c.getStatus(),
                signed(c.getLessorSigned()), signed(c.getTenantSigned()), c.getStartDate(), c.getEndDate(),
                c.getLeaseMonths(), c.getMonthlyRent(), c.getDeposit(), c.getServiceFee(), c.getPaymentMonths(),
                c.getFirstPaymentAmount(), c.getDocTemplateId(), c.getTemplateConfigId(), c.getTemplateVersion(),
                c.getContractFileId(), c.getSignFlowId(), StringUtils.hasText(c.getContractFileId()),
                c.getFailureCode(), c.getFailureMessage(),
                c.getSignedAt(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private boolean signed(Integer value) { return value != null && value == 1; }
}
