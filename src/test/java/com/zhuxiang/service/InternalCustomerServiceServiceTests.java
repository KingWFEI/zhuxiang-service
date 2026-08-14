package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.mapper.*;
import com.zhuxiang.service.service.AppointmentService;
import com.zhuxiang.service.service.LeaseTerminationService;
import com.zhuxiang.service.service.impl.InternalCustomerServiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalCustomerServiceServiceTests {

    private HouseMapper houseMapper;
    private PaymentRecordMapper paymentRecordMapper;
    private InternalCustomerServiceServiceImpl service;

    @BeforeEach
    void setUp() {
        houseMapper = mock(HouseMapper.class);
        paymentRecordMapper = mock(PaymentRecordMapper.class);
        service = new InternalCustomerServiceServiceImpl(
                mock(LeaseMapper.class),
                mock(RentBillMapper.class),
                mock(LockPermissionMapper.class),
                mock(LockDeviceMapper.class),
                mock(SmartLockMapper.class),
                mock(AppointmentMapper.class),
                mock(RepairRecordMapper.class),
                houseMapper,
                mock(CommunityMapper.class),
                mock(RentOrderMapper.class),
                paymentRecordMapper,
                mock(DepositRecordMapper.class),
                mock(LeaseTerminationApplicationMapper.class),
                mock(RentContractMapper.class),
                mock(UserMapper.class),
                mock(UserRealNameAuthMapper.class),
                mock(LandlordAuthApplicationMapper.class),
                mock(AppointmentService.class),
                mock(LeaseTerminationService.class)
        );
    }

    @Test
    void rejectsPaymentDetailOwnedByAnotherUser() {
        PaymentRecord payment = new PaymentRecord();
        payment.setId("payment-1");
        payment.setUserId("other-user");
        when(paymentRecordMapper.selectById("payment-1")).thenReturn(payment);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getPaymentDetail("user-1", "payment-1")
        );

        assertEquals(403, exception.getCode());
    }

    @Test
    void mapsHouseSearchToSafeCustomerServiceFields() {
        House house = new House();
        house.setId("house-1");
        house.setTitle("朝阳两居室");
        house.setStatus("available");
        house.setRoomType("2室1厅");
        house.setArea(new BigDecimal("68.5"));
        house.setPrice(500000);
        house.setIsSmartLockSupported(1);
        house.setIsSelfViewingSupported(1);
        when(houseMapper.selectList(any(Wrapper.class))).thenReturn(List.of(house));

        var result = service.searchHouses("朝阳区", "2室", null, 500000, 5);

        assertEquals(1, result.size());
        assertEquals("house-1", result.getFirst().houseId());
        assertEquals(500000, result.getFirst().monthlyRent());
        assertTrue(result.getFirst().smartLockSupported());
        assertTrue(result.getFirst().selfViewingSupported());
    }
}
