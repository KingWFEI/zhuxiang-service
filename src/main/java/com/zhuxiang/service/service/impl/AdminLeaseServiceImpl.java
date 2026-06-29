package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminLeaseDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.LeaseMapper;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.service.AdminLeaseService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminLeaseServiceImpl implements AdminLeaseService {

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "HOUSEKEEPER", "LANDLORD");
    private static final Set<String> LEASE_STATUSES = Set.of("pending", "active", "expired", "terminated");

    private final LeaseMapper leaseMapper;
    private final UserService userService;
    private final HouseService houseService;
    private final RentContractMapper rentContractMapper;

    public AdminLeaseServiceImpl(
            LeaseMapper leaseMapper,
            UserService userService,
            HouseService houseService,
            RentContractMapper rentContractMapper
    ) {
        this.leaseMapper = leaseMapper;
        this.userService = userService;
        this.houseService = houseService;
        this.rentContractMapper = rentContractMapper;
    }

    @Override
    public PageData<AdminLeaseDtos.AdminLeaseView> getLeases(
            String operatorId,
            String status,
            String keyword,
            long page,
            long pageSize
    ) {
        requireAdminRole(operatorId);

        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toLowerCase() : null;
        if (normalizedStatus != null && !LEASE_STATUSES.contains(normalizedStatus)) {
            throw BusinessException.badRequest("不支持的租约状态");
        }

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        LambdaQueryWrapper<Lease> query = Wrappers.<Lease>lambdaQuery()
                .eq(normalizedStatus != null, Lease::getStatus, normalizedStatus)
                .orderByDesc(Lease::getCreatedAt);

        if (normalizedKeyword != null) {
            List<String> tenantIds = findTenantIds(normalizedKeyword);
            List<String> houseIds = findHouseIds(normalizedKeyword);
            query.and(wrapper -> {
                wrapper.like(Lease::getId, normalizedKeyword);
                if (!tenantIds.isEmpty()) {
                    wrapper.or().in(Lease::getUserId, tenantIds);
                }
                if (!houseIds.isEmpty()) {
                    wrapper.or().in(Lease::getHouseId, houseIds);
                }
            });
        }

        IPage<Lease> result = leaseMapper.selectPage(new Page<>(page, pageSize), query);
        Map<String, User> tenants = loadTenants(result.getRecords());
        Map<String, House> houses = loadHouses(result.getRecords());
        Map<String, RentContract> contracts = loadContracts(result.getRecords());

        List<AdminLeaseDtos.AdminLeaseView> items = result.getRecords().stream()
                .map(lease -> toView(lease, tenants, houses, contracts))
                .toList();
        return PageData.of(items, page, pageSize, result.getTotal());
    }

    private List<String> findTenantIds(String keyword) {
        return userService.list(
                Wrappers.<User>lambdaQuery()
                        .select(User::getId)
                        .and(wrapper -> wrapper.like(User::getNickname, keyword)
                                .or().like(User::getPhone, keyword))
        ).stream().map(User::getId).toList();
    }

    private List<String> findHouseIds(String keyword) {
        return houseService.list(
                Wrappers.<House>lambdaQuery()
                        .select(House::getId)
                        .and(wrapper -> wrapper.like(House::getTitle, keyword)
                                .or().like(House::getAddress, keyword)
                                .or().like(House::getBuilding, keyword)
                                .or().like(House::getUnit, keyword)
                                .or().like(House::getRoom, keyword))
        ).stream().map(House::getId).toList();
    }

    private Map<String, User> loadTenants(List<Lease> leases) {
        Set<String> ids = leases.stream().map(Lease::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Collections.emptyMap() : userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<String, House> loadHouses(List<Lease> leases) {
        Set<String> ids = leases.stream().map(Lease::getHouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Collections.emptyMap() : houseService.listByIds(ids).stream()
                .collect(Collectors.toMap(House::getId, Function.identity()));
    }

    private Map<String, RentContract> loadContracts(List<Lease> leases) {
        Set<String> ids = leases.stream().map(Lease::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Collections.emptyMap() : rentContractMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(RentContract::getId, Function.identity()));
    }

    private AdminLeaseDtos.AdminLeaseView toView(
            Lease lease,
            Map<String, User> tenants,
            Map<String, House> houses,
            Map<String, RentContract> contracts
    ) {
        User tenant = tenants.get(lease.getUserId());
        House house = houses.get(lease.getHouseId());
        RentContract contract = contracts.get(lease.getContractId());
        return new AdminLeaseDtos.AdminLeaseView(
                lease.getId(), lease.getUserId(),
                tenant == null ? null : tenant.getNickname(),
                tenant == null ? null : tenant.getPhone(),
                lease.getHouseId(),
                house == null ? null : house.getTitle(),
                house == null ? null : house.getAddress(),
                lease.getStatus(), lease.getStartDate(), lease.getEndDate(), lease.getLeaseMonths(),
                lease.getPaymentMethod(), lease.getPaymentMonths(), lease.getMonthlyRent(), lease.getDeposit(),
                lease.getServiceFee(), lease.getFirstPaymentAmount(), lease.getContractId(),
                contract == null ? null : contract.getContractNo(),
                contract == null ? null : contract.getStatus(),
                lease.getCreatedAt(), lease.getUpdatedAt()
        );
    }

    private void requireAdminRole(String operatorId) {
        User user = userService.requireActiveUser(operatorId);
        if (!ADMIN_ROLES.contains(user.getRole())) {
            throw BusinessException.forbidden("当前账号无权查看管理端租约");
        }
    }
}
