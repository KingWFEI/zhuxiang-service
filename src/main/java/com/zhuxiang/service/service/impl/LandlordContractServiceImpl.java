package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.ContractPreviewResponse;
import com.zhuxiang.service.dto.EsignSignResponse;
import com.zhuxiang.service.dto.EsignSignStatusResponse;
import com.zhuxiang.service.dto.LandlordContractDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LandlordContractService;
import com.zhuxiang.service.service.RentOrderService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LandlordContractServiceImpl implements LandlordContractService {

    private final RentOrderMapper rentOrderMapper;
    private final RentContractMapper rentContractMapper;
    private final HouseService houseService;
    private final UserService userService;
    private final RentOrderService rentOrderService;

    public LandlordContractServiceImpl(
            RentOrderMapper rentOrderMapper,
            RentContractMapper rentContractMapper,
            HouseService houseService,
            UserService userService,
            RentOrderService rentOrderService
    ) {
        this.rentOrderMapper = rentOrderMapper;
        this.rentContractMapper = rentContractMapper;
        this.houseService = houseService;
        this.userService = userService;
        this.rentOrderService = rentOrderService;
    }

    @Override
    public PageData<LandlordContractDtos.ContractItem> listPendingSign(
            String landlordUserId, long page, long pageSize
    ) {
        requireLandlordRole(landlordUserId);
        validatePage(page, pageSize);

        var result = rentOrderMapper.selectLandlordPendingSignPage(
                new Page<>(page, pageSize), landlordUserId);
        Map<String, RentContract> contracts = loadContracts(result.getRecords());
        Map<String, House> houses = loadHouses(result.getRecords());

        List<LandlordContractDtos.ContractItem> items = result.getRecords().stream()
                .map(order -> toItem(order, contracts.get(order.getId()), houses.get(order.getHouseId())))
                .filter(Objects::nonNull)
                .toList();
        return PageData.of(items, page, pageSize, result.getTotal());
    }

    @Override
    public LandlordContractDtos.ContractDetail getDetail(String landlordUserId, String orderId) {
        requireLandlordRole(landlordUserId);
        RentOrder order = requireLandlordOrder(landlordUserId, orderId);
        RentContract contract = requireContract(orderId);
        ContractPreviewResponse preview = rentOrderService.getContractPreview(landlordUserId, orderId);
        return new LandlordContractDtos.ContractDetail(
                order.getId(), contract.getId(), contract.getStatus(),
                signed(contract.getTenantSigned()), signed(contract.getLessorSigned()),
                signStage(contract), preview);
    }

    @Override
    public EsignSignResponse sign(String landlordUserId, String orderId) {
        requireLandlordRole(landlordUserId);
        requireLandlordOrder(landlordUserId, orderId);
        return rentOrderService.sign(landlordUserId, orderId);
    }

    @Override
    public EsignSignStatusResponse refresh(String landlordUserId, String orderId) {
        requireLandlordRole(landlordUserId);
        requireLandlordOrder(landlordUserId, orderId);
        return rentOrderService.contractRefresh(landlordUserId, orderId);
    }

    private void requireLandlordRole(String userId) {
        User user = userService.getById(userId);
        if (user == null || !"LANDLORD".equalsIgnoreCase(user.getRole())) {
            throw BusinessException.forbidden("仅房东可以操作合同工作台");
        }
    }

    private RentOrder requireLandlordOrder(String landlordUserId, String orderId) {
        RentOrder order = rentOrderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!landlordUserId.equals(order.getLessorUserId())) {
            throw BusinessException.forbidden("无权操作该合同");
        }
        return order;
    }

    private RentContract requireContract(String orderId) {
        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getOrderId, orderId)
                        .last("LIMIT 1"), false);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        return contract;
    }

    private Map<String, RentContract> loadContracts(List<RentOrder> orders) {
        Set<String> orderIds = orders.stream().map(RentOrder::getId).collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return rentContractMapper.selectList(
                Wrappers.<RentContract>lambdaQuery().in(RentContract::getOrderId, orderIds)
        ).stream().collect(Collectors.toMap(RentContract::getOrderId, Function.identity()));
    }

    private Map<String, House> loadHouses(List<RentOrder> orders) {
        Set<String> houseIds = orders.stream()
                .map(RentOrder::getHouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (houseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return houseService.listByIds(houseIds).stream()
                .collect(Collectors.toMap(House::getId, Function.identity()));
    }

    private LandlordContractDtos.ContractItem toItem(
            RentOrder order, RentContract contract, House house
    ) {
        if (contract == null) {
            return null;
        }
        return new LandlordContractDtos.ContractItem(
                order.getId(), contract.getId(), contract.getContractNo(), contract.getStatus(),
                signed(contract.getTenantSigned()), signed(contract.getLessorSigned()), signStage(contract),
                order.getHouseId(), contract.getHouseName(), contract.getRoomName(),
                contract.getHouseAddress() != null ? contract.getHouseAddress()
                        : house != null ? house.getAddress() : "",
                contract.getTenantName(), maskPhone(contract.getTenantPhone()),
                contract.getStartDate(), contract.getEndDate(), contract.getMonthlyRent(),
                contract.getDeposit(), contract.getUpdatedAt());
    }

    private String signStage(RentContract contract) {
        if (signed(contract.getTenantSigned()) && signed(contract.getLessorSigned())) {
            return "COMPLETED";
        }
        return signed(contract.getLessorSigned()) ? "WAITING_OTHER_SIGNATURE" : "WAITING_MY_SIGNATURE";
    }

    private boolean signed(Integer value) {
        return Integer.valueOf(1).equals(value);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private void validatePage(long page, long pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw BusinessException.badRequest("分页参数不正确");
        }
    }
}
