package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminBillDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.entity.RentBill;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.RentBillMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminBillServiceImpl;
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

class AdminBillServiceTests {

    private final RentBillMapper rentBillMapper = mock(RentBillMapper.class);
    private final LeaseService leaseService = mock(LeaseService.class);
    private final UserService userService = mock(UserService.class);
    private final HouseService houseService = mock(HouseService.class);
    private final PaymentRecordService paymentRecordService = mock(PaymentRecordService.class);

    private AdminBillServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminBillServiceImpl(
                rentBillMapper, leaseService, userService, houseService, paymentRecordService
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void returnsPagedBillsWithLeaseTenantHouseAndPaymentData() {
        User admin = user("admin-1", "ADMIN");
        User tenant = user("tenant-1", "TENANT");
        tenant.setNickname("张三");
        tenant.setPhone("13800138000");

        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setUserId(tenant.getId());
        lease.setHouseId("house-1");
        lease.setStatus("active");

        House house = new House();
        house.setId("house-1");
        house.setTitle("阳光花园 1 栋 101");
        house.setAddress("重庆市渝北区测试路 1 号");

        RentBill bill = new RentBill();
        bill.setId("bill-1");
        bill.setLeaseId(lease.getId());
        bill.setPeriodNo(2);
        bill.setAmountDue(300000);
        bill.setAmountPaid(0);
        bill.setOverdueAmount(1500);
        bill.setDueDate(LocalDate.of(2026, 8, 1));
        bill.setStatus("overdue");

        PaymentRecord payment = new PaymentRecord();
        payment.setBillId(bill.getId());
        payment.setPaymentNo("PAY-001");
        payment.setPaymentChannel("alipay");
        payment.setStatus("pending");
        payment.setCreatedAt(LocalDateTime.of(2026, 8, 1, 10, 0));

        Page<RentBill> databasePage = new Page<>(1, 20, 1);
        databasePage.setRecords(List.of(bill));
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        when(rentBillMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(databasePage);
        when(leaseService.listByIds(any(Collection.class))).thenReturn(List.of(lease));
        when(userService.listByIds(any(Collection.class))).thenReturn(List.of(tenant));
        when(houseService.listByIds(any(Collection.class))).thenReturn(List.of(house));
        when(paymentRecordService.list(any(Wrapper.class))).thenReturn(List.of(payment));

        PageData<AdminBillDtos.BillView> result = service.getBills(
                "admin-1", "OVERDUE", null, null, null, 1, 20
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.billId()).isEqualTo("bill-1");
            assertThat(item.tenantName()).isEqualTo("张三");
            assertThat(item.houseName()).isEqualTo("阳光花园 1 栋 101");
            assertThat(item.paymentNo()).isEqualTo("PAY-001");
            assertThat(item.outstandingAmount()).isEqualTo(301500);
        });
    }

    @Test
    void returnsDatabaseSummary() {
        AdminBillDtos.BillSummary expected = new AdminBillDtos.BillSummary(
                12, 3, 2, 5, 1, 1, 3600000, 1500000, 2100000, 301500
        );
        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN"));
        when(rentBillMapper.selectAdminSummary(null)).thenReturn(expected);

        assertThat(service.getSummary("admin-1")).isSameAs(expected);
        verify(rentBillMapper).selectAdminSummary(null);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void resolvesInitialRentPaymentThroughLeaseOrderForPaidBill() {
        User admin = user("admin-1", "ADMIN");
        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setOrderId("order-1");

        RentBill bill = new RentBill();
        bill.setId("bill-1");
        bill.setLeaseId("lease-1");
        bill.setAmountDue(300000);
        bill.setAmountPaid(300000);
        bill.setStatus("paid");

        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId("order-1");
        payment.setPaymentNo("ORDER-PAY-001");
        payment.setType("rent");
        payment.setStatus("success");
        payment.setPaidAt(LocalDateTime.of(2026, 8, 1, 9, 30));

        Page<RentBill> databasePage = new Page<>(1, 20, 1);
        databasePage.setRecords(List.of(bill));
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        when(rentBillMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(databasePage);
        when(leaseService.listByIds(any(Collection.class))).thenReturn(List.of(lease));
        when(paymentRecordService.list(any(Wrapper.class))).thenReturn(List.of(payment));

        AdminBillDtos.BillView item = service.getBills(
                "admin-1", null, null, null, null, 1, 20
        ).items().getFirst();

        assertThat(item.paymentNo()).isEqualTo("ORDER-PAY-001");
        assertThat(item.paidAt()).isEqualTo(LocalDateTime.of(2026, 8, 1, 9, 30));
    }

    @Test
    void rejectsTenantAndInvalidDateRangeBeforeQueryingBills() {
        when(userService.requireActiveUser("tenant-1")).thenReturn(user("tenant-1", "TENANT"));
        assertThatThrownBy(() -> service.getBills(
                "tenant-1", null, null, null, null, 1, 20
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(403));

        when(userService.requireActiveUser("admin-1")).thenReturn(user("admin-1", "ADMIN"));
        assertThatThrownBy(() -> service.getBills(
                "admin-1", null, null,
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1), 1, 20
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(400));

        verify(rentBillMapper, never()).selectPage(any(), any());
    }

    private User user(String id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus("active");
        return user;
    }
}
