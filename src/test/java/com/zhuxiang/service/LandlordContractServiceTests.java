package com.zhuxiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.EsignSignResponse;
import com.zhuxiang.service.dto.EsignSignStatusResponse;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.RentOrderService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.LandlordContractServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LandlordContractServiceTests {

    private static final String LANDLORD_ID = "landlord-user-1";
    private static final String ORDER_ID = "order-1";

    private final RentOrderMapper rentOrderMapper = mock(RentOrderMapper.class);
    private final RentContractMapper rentContractMapper = mock(RentContractMapper.class);
    private final HouseService houseService = mock(HouseService.class);
    private final UserService userService = mock(UserService.class);
    private final RentOrderService rentOrderService = mock(RentOrderService.class);

    private LandlordContractServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LandlordContractServiceImpl(
                rentOrderMapper, rentContractMapper, houseService, userService, rentOrderService);
        User landlord = new User();
        landlord.setId(LANDLORD_ID);
        landlord.setRole("LANDLORD");
        when(userService.getById(LANDLORD_ID)).thenReturn(landlord);
    }

    @Test
    void pendingListIncludesContractBeforeTenantSigns() {
        RentOrder order = order(LANDLORD_ID);
        Page<RentOrder> result = new Page<>(1, 20);
        result.setRecords(List.of(order));
        result.setTotal(1);
        when(rentOrderMapper.selectLandlordPendingSignPage(any(Page.class), eq(LANDLORD_ID)))
                .thenReturn(result);

        RentContract contract = contract();
        contract.setTenantSigned(0);
        contract.setLessorSigned(0);
        when(rentContractMapper.selectList(any())).thenReturn(List.of(contract));

        House house = new House();
        house.setId("house-1");
        house.setAddress("测试地址");
        when(houseService.listByIds(anyCollection())).thenReturn(List.of(house));

        var page = service.listPendingSign(LANDLORD_ID, 1, 20);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.orderId()).isEqualTo(ORDER_ID);
            assertThat(item.tenantSigned()).isFalse();
            assertThat(item.lessorSigned()).isFalse();
            assertThat(item.signStage()).isEqualTo("WAITING_MY_SIGNATURE");
            assertThat(item.tenantPhone()).isEqualTo("138****8000");
        });
    }

    @Test
    void landlordCanReuseExistingSignFlowEntry() {
        when(rentOrderMapper.selectById(ORDER_ID)).thenReturn(order(LANDLORD_ID));
        EsignSignResponse expected = new EsignSignResponse(
                "SIGNING", "LESSOR", false, "https://sign.example");
        when(rentOrderService.sign(LANDLORD_ID, ORDER_ID)).thenReturn(expected);

        assertThat(service.sign(LANDLORD_ID, ORDER_ID)).isSameAs(expected);
        verify(rentOrderService).sign(LANDLORD_ID, ORDER_ID);
    }

    @Test
    void landlordCannotSignAnotherLandlordsOrder() {
        when(rentOrderMapper.selectById(ORDER_ID)).thenReturn(order("another-landlord"));

        assertThatThrownBy(() -> service.sign(LANDLORD_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
        verify(rentOrderService, never()).sign(any(), any());
    }

    @Test
    void refreshDelegatesToSharedCompletionFlow() {
        when(rentOrderMapper.selectById(ORDER_ID)).thenReturn(order(LANDLORD_ID));
        EsignSignStatusResponse expected = new EsignSignStatusResponse(
                "COMPLETED", true, true, true, true, LocalDateTime.now());
        when(rentOrderService.contractRefresh(LANDLORD_ID, ORDER_ID)).thenReturn(expected);

        assertThat(service.refresh(LANDLORD_ID, ORDER_ID)).isSameAs(expected);
        verify(rentOrderService).contractRefresh(LANDLORD_ID, ORDER_ID);
    }

    @Test
    void tenantRoleCannotAccessLandlordWorkbench() {
        User tenant = new User();
        tenant.setId("tenant-1");
        tenant.setRole("TENANT");
        when(userService.getById("tenant-1")).thenReturn(tenant);

        assertThatThrownBy(() -> service.listPendingSign("tenant-1", 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅房东");
        verify(rentOrderMapper, never()).selectLandlordPendingSignPage(any(), any());
    }

    private RentOrder order(String lessorUserId) {
        RentOrder order = new RentOrder();
        order.setId(ORDER_ID);
        order.setUserId("tenant-1");
        order.setLessorUserId(lessorUserId);
        order.setHouseId("house-1");
        order.setStatus("pendingLandlordSign");
        return order;
    }

    private RentContract contract() {
        RentContract contract = new RentContract();
        contract.setId("contract-1");
        contract.setOrderId(ORDER_ID);
        contract.setContractNo("RC20260717001");
        contract.setStatus("signing");
        contract.setHouseName("测试房源");
        contract.setRoomName("1栋101室");
        contract.setTenantName("租客甲");
        contract.setTenantPhone("13800138000");
        contract.setStartDate(LocalDate.now().plusDays(1));
        contract.setEndDate(LocalDate.now().plusYears(1));
        contract.setMonthlyRent(300000);
        contract.setDeposit(300000);
        contract.setUpdatedAt(LocalDateTime.now());
        return contract;
    }
}
