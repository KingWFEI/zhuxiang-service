package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminOrderServiceTests {
    private final RentOrderMapper orderMapper = mock(RentOrderMapper.class);
    private final RentContractMapper contractMapper = mock(RentContractMapper.class);
    private final HouseService houseService = mock(HouseService.class);
    private final UserService userService = mock(UserService.class);
    private final PaymentRecordService paymentService = mock(PaymentRecordService.class);
    private final AdminOrderServiceImpl service = new AdminOrderServiceImpl(
            orderMapper, contractMapper, houseService, userService, paymentService);

    @BeforeEach
    void setUp() {
        User admin = new User(); admin.setId("admin-1"); admin.setRole("ADMIN");
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_shouldReturnRealRentOrders() {
        RentOrder order = new RentOrder(); order.setId("order-1"); order.setStatus("pendingPayment");
        order.setTenantName("测试租客"); order.setFirstPaymentAmount(420000); order.setCreatedAt(LocalDateTime.now());
        Page<RentOrder> result = new Page<>(1, 20, 1); result.setRecords(List.of(order));
        when(orderMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(result);
        when(contractMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(paymentService.list(any(Wrapper.class))).thenReturn(List.of());

        var page = service.list("admin-1", "pendingPayment", null, 1, 20);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items().get(0).id()).isEqualTo("order-1");
        assertThat(page.items().get(0).firstPaymentAmount()).isEqualTo(420000);
    }

    @Test
    void list_shouldRejectUnknownStatus() {
        assertThatThrownBy(() -> service.list("admin-1", "unknown", null, 1, 20))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不支持的订单状态");
    }

    @Test
    void landlord_shouldNotSeeAllOrders() {
        User landlord = new User(); landlord.setId("landlord-1"); landlord.setRole("LANDLORD");
        when(userService.requireActiveUser("landlord-1")).thenReturn(landlord);
        assertThatThrownBy(() -> service.list("landlord-1", null, null, 1, 20))
                .isInstanceOf(BusinessException.class).hasMessageContaining("无权查看全部订单");
    }
}
