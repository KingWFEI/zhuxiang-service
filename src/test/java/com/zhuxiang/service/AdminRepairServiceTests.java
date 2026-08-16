package com.zhuxiang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.RepairDtos.AssignRepairRequest;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.RepairRecord;
import com.zhuxiang.service.entity.RepairLog;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.RepairLogMapper;
import com.zhuxiang.service.mapper.RepairRecordMapper;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.RepairRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class AdminRepairServiceTests {

    private final RepairLogMapper logMapper = mock(RepairLogMapper.class);
    private final RepairRecordMapper recordMapper = mock(RepairRecordMapper.class);
    private final UserService userService = mock(UserService.class);
    private final HouseService houseService = mock(HouseService.class);
    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private RepairRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new RepairRecordServiceImpl(
                logMapper, new ObjectMapper(), userService, houseService, fileRecordService));
        ReflectionTestUtils.setField(service, "baseMapper", recordMapper);
        when(recordMapper.updateStatusIfCurrent(any(), any(), any(), any())).thenReturn(1);
        when(recordMapper.assignIfCurrent(any(), any(), any(), any())).thenReturn(1);
        when(recordMapper.finishIfProcessing(any(), any())).thenReturn(1);
        when(userService.requireActiveUser("admin-1")).thenReturn(admin());
    }

    @Test
    void followsAdminRepairStateMachineAndExposesAvailableActions() {
        RepairRecord record = record("submitted");
        doReturn(record).when(service).getById("repair-1");
        when(userService.getById("tenant-1")).thenReturn(user());
        when(houseService.getById("house-1")).thenReturn(house());

        var accepted = service.acceptAdminRepair("admin-1", "repair-1");
        assertThat(accepted.status()).isEqualTo("accepted");
        assertThat(accepted.availableActions()).containsExactly("assign");

        var assigned = service.assignAdminRepair("admin-1", "repair-1", new AssignRepairRequest("王师傅", null));
        assertThat(assigned.status()).isEqualTo("assigned");
        assertThat(assigned.repairmanName()).isEqualTo("王师傅");

        service.startAdminRepair("admin-1", "repair-1");
        var finished = service.finishAdminRepair("admin-1", "repair-1");
        assertThat(finished.status()).isEqualTo("pendingReview");
        assertThat(finished.completedAt()).isNotNull();
        verify(logMapper, org.mockito.Mockito.times(4)).insert(any(RepairLog.class));
    }

    @Test
    void rejectsInvalidTransition() {
        RepairRecord record = record("submitted");
        doReturn(record).when(service).getById("repair-1");
        assertThatThrownBy(() -> service.finishAdminRepair("admin-1", "repair-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(409));
    }

    @Test
    void rejectsConcurrentStatusChangeWithoutWritingLog() {
        RepairRecord record = record("submitted");
        doReturn(record).when(service).getById("repair-1");
        when(recordMapper.updateStatusIfCurrent(any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.acceptAdminRepair("admin-1", "repair-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(409));
        verify(logMapper, never()).insert(any(RepairLog.class));
    }

    @Test
    void preventsLandlordFromOpeningAnotherLandlordsRepair() {
        RepairRecord record = record("submitted");
        doReturn(record).when(service).getById("repair-1");
        User landlord = new User();
        landlord.setId("landlord-1");
        landlord.setRole("LANDLORD");
        landlord.setStatus("active");
        when(userService.requireActiveUser("landlord-1")).thenReturn(landlord);
        House house = house();
        house.setLandlordId("landlord-2");
        when(houseService.getById("house-1")).thenReturn(house);

        assertThatThrownBy(() -> service.getAdminRepairDetail("landlord-1", "repair-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(403));
    }

    private RepairRecord record(String status) {
        RepairRecord record = new RepairRecord();
        record.setId("repair-1");
        record.setOrderNo("BX202608150001");
        record.setUserId("tenant-1");
        record.setHouseId("house-1");
        record.setHouseName("测试房源");
        record.setContactName("张三");
        record.setContactPhone("13800138000");
        record.setRepairType("plumbing");
        record.setDescription("漏水");
        record.setStatus(status);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        return record;
    }

    private User user() {
        User user = new User();
        user.setId("tenant-1");
        user.setNickname("张三");
        user.setPhone("13800138000");
        return user;
    }

    private User admin() {
        User user = new User();
        user.setId("admin-1");
        user.setRole("ADMIN");
        user.setStatus("active");
        return user;
    }

    private House house() {
        House house = new House();
        house.setId("house-1");
        house.setAddress("测试地址");
        return house;
    }
}
