package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.LeaseDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.LockPermission;
import com.zhuxiang.service.entity.RentBill;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.mapper.LeaseMapper;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.service.LockPasscodePermissionService;
import com.zhuxiang.service.service.LockPermissionService;
import com.zhuxiang.service.service.RentBillService;
import com.zhuxiang.service.service.impl.LeaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaseDetailServiceTests {

    private final HouseService houseService = mock(HouseService.class);
    private final CommunityService communityService = mock(CommunityService.class);
    private final SmartLockMapper smartLockMapper = mock(SmartLockMapper.class);
    private final LockPermissionService lockPermissionService = mock(LockPermissionService.class);
    private final LockPasscodePermissionService passcodePermissionService = mock(LockPasscodePermissionService.class);
    private final RentContractMapper contractMapper = mock(RentContractMapper.class);
    private final RentBillService billService = mock(RentBillService.class);
    private final LandlordService landlordService = mock(LandlordService.class);
    private final LeaseMapper leaseMapper = mock(LeaseMapper.class);
    private LeaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LeaseServiceImpl(
                houseService, communityService, smartLockMapper, lockPermissionService,
                passcodePermissionService, contractMapper, billService, landlordService
        );
        ReflectionTestUtils.setField(service, "baseMapper", leaseMapper);
    }

    @Test
    void returnsOwnedLeaseWithContractBillAndLockDetails() {
        Lease lease = lease();
        House house = house();
        RentContract contract = contract();
        RentBill bill = bill();
        Landlord keeper = new Landlord();
        keeper.setId("keeper-1");
        keeper.setName("小住管家");
        keeper.setPhone("400-800-2026");
        SmartLock smartLock = new SmartLock();
        smartLock.setId("lock-1");
        LockPermission permission = new LockPermission();
        permission.setStatus("ACTIVE");

        when(leaseMapper.selectById("lease-1")).thenReturn(lease);
        when(houseService.getById("house-1")).thenReturn(house);
        when(contractMapper.selectById("contract-1")).thenReturn(contract);
        when(landlordService.getById("keeper-1")).thenReturn(keeper);
        when(billService.getOne(any(Wrapper.class), eq(false))).thenReturn(bill);
        when(smartLockMapper.selectLatestByHouseId("house-1")).thenReturn(smartLock);
        when(lockPermissionService.getOne(any(Wrapper.class), eq(false))).thenReturn(permission);

        LeaseDtos.LeaseDetail result = service.getLeaseDetail("lease-1", "tenant-1");

        assertThat(result.id()).isEqualTo("lease-1");
        assertThat(result.houseName()).isEqualTo("3栋2单元1201");
        assertThat(result.houseSummary()).isEqualTo("温馨一居 · 42㎡ · 朝南");
        assertThat(result.tenantName()).isEqualTo("王小明");
        assertThat(result.monthlyRent()).isEqualByComparingTo("2680.00");
        assertThat(result.deposit()).isEqualByComparingTo("2680.00");
        assertThat(result.paymentDay()).isEqualTo(5);
        assertThat(result.billStatus()).isEqualTo("unpaid");
        assertThat(result.lockPermissionStatus()).isEqualTo("active");
        assertThat(result.pendingBillTitle()).isEqualTo("3月租金待支付");
        assertThat(result.pendingBillAmount()).isEqualByComparingTo("2680.00");
        assertThat(result.pendingBillDueDate()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void rejectsAnotherTenantsLease() {
        when(leaseMapper.selectById("lease-1")).thenReturn(lease());

        assertThatThrownBy(() -> service.getLeaseDetail("lease-1", "other-tenant"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(403));
    }

    @Test
    void returnsLockDataForOwnedLeaseUnlockData() {
        SmartLock smartLock = new SmartLock();
        smartLock.setId("lock-1");
        smartLock.setLockName("1201门锁");
        smartLock.setLockMac("AA:BB:CC:DD:EE:FF");
        smartLock.setLockData("encrypted-sdk-lock-data");

        when(leaseMapper.selectById("lease-1")).thenReturn(lease());
        when(houseService.getById("house-1")).thenReturn(house());
        when(smartLockMapper.selectLatestByHouseId("house-1")).thenReturn(smartLock);

        LeaseDtos.UnlockDataResponse result = service.getUnlockData("lease-1", "tenant-1");

        assertThat(result.lockData()).isEqualTo("encrypted-sdk-lock-data");
    }

    private Lease lease() {
        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setContractId("contract-1");
        lease.setHouseId("house-1");
        lease.setUserId("tenant-1");
        lease.setStartDate(LocalDate.of(2026, 3, 1));
        lease.setEndDate(LocalDate.of(2027, 2, 28));
        lease.setMonthlyRent(268000);
        lease.setDeposit(268000);
        lease.setStatus("active");
        return lease;
    }

    private House house() {
        House house = new House();
        house.setId("house-1");
        house.setBuilding("3栋");
        house.setUnit("2单元");
        house.setRoom("1201");
        house.setAddress("重庆市渝北区中央公园悦居社区");
        house.setRoomType("温馨一居");
        house.setArea(new BigDecimal("42.00"));
        house.setOrientation("朝南");
        house.setPaymentMethod("押一付一");
        house.setLandlordId("keeper-1");
        return house;
    }

    private RentContract contract() {
        RentContract contract = new RentContract();
        contract.setId("contract-1");
        contract.setStatus("signed");
        contract.setTenantName("王小明");
        contract.setTenantPhone("13800138000");
        contract.setTenantIdCard("500101199605201234");
        return contract;
    }

    private RentBill bill() {
        RentBill bill = new RentBill();
        bill.setId("bill-1");
        bill.setLeaseId("lease-1");
        bill.setAmountDue(268000);
        bill.setDueDate(LocalDate.of(2026, 3, 5));
        bill.setStatus("pending");
        return bill;
    }
}
