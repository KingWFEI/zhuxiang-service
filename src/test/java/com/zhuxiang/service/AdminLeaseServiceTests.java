package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminLeaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminLeaseServiceTests {

    private final LeaseMapper leaseMapper = mock(LeaseMapper.class);
    private final UserService userService = mock(UserService.class);
    private final HouseService houseService = mock(HouseService.class);
    private final RentContractMapper rentContractMapper = mock(RentContractMapper.class);

    private AdminLeaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminLeaseServiceImpl(leaseMapper, userService, houseService, rentContractMapper);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void returnsPagedLeaseWithRelatedManagementFields() {
        User admin = user("admin-1", "ADMIN", "管理员", "13000000000");
        User tenant = user("tenant-1", "TENANT", "张三", "13800138000");
        House house = new House();
        house.setId("house-1");
        house.setTitle("阳光花园 1 栋 101");
        house.setAddress("重庆市渝北区测试路 1 号");

        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setUserId(tenant.getId());
        lease.setHouseId(house.getId());
        lease.setStatus("active");
        lease.setStartDate(LocalDate.of(2026, 6, 1));
        lease.setEndDate(LocalDate.of(2027, 5, 31));
        lease.setLeaseMonths(12);
        lease.setMonthlyRent(300000);
        lease.setDeposit(300000);
        lease.setContractId("contract-1");
        lease.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));

        RentContract contract = new RentContract();
        contract.setId("contract-1");
        contract.setContractNo("HT-20260601-001");
        contract.setStatus("signed");

        Page<Lease> databasePage = new Page<>(1, 20, 1);
        databasePage.setRecords(List.of(lease));
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        when(leaseMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(databasePage);
        when(userService.listByIds(any(Collection.class))).thenReturn(List.of(tenant));
        when(houseService.listByIds(any(Collection.class))).thenReturn(List.of(house));
        when(rentContractMapper.selectByIds(any(Collection.class))).thenReturn(List.of(contract));

        PageData<AdminLeaseDtos.AdminLeaseView> result = service.getLeases(
                "admin-1", "ACTIVE", null, 1, 20
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.hasMore()).isFalse();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.leaseId()).isEqualTo("lease-1");
            assertThat(item.tenantName()).isEqualTo("张三");
            assertThat(item.houseName()).isEqualTo("阳光花园 1 栋 101");
            assertThat(item.contractNo()).isEqualTo("HT-20260601-001");
            assertThat(item.contractStatus()).isEqualTo("signed");
        });
    }

    @Test
    void rejectsTenantAccount() {
        when(userService.requireActiveUser("tenant-1"))
                .thenReturn(user("tenant-1", "TENANT", "张三", "13800138000"));

        assertThatThrownBy(() -> service.getLeases("tenant-1", null, null, 1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
        verify(leaseMapper, never()).selectPage(any(), any());
    }

    @Test
    void rejectsUnsupportedStatus() {
        when(userService.requireActiveUser("admin-1"))
                .thenReturn(user("admin-1", "ADMIN", "管理员", "13000000000"));

        assertThatThrownBy(() -> service.getLeases("admin-1", "unknown", null, 1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
        verify(leaseMapper, never()).selectPage(any(), any());
    }

    private User user(String id, String role, String nickname, String phone) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setStatus("active");
        return user;
    }
}
