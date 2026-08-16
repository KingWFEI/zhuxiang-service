package com.zhuxiang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.RepairDtos;
import com.zhuxiang.service.entity.RepairLog;
import com.zhuxiang.service.entity.RepairRecord;
import com.zhuxiang.service.mapper.RepairLogMapper;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.RepairRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepairRecordServiceTests {

    private final RepairLogMapper repairLogMapper = mock(RepairLogMapper.class);
    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private RepairRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new RepairRecordServiceImpl(
                repairLogMapper,
                new ObjectMapper(),
                mock(UserService.class),
                mock(HouseService.class),
                fileRecordService
        ));
        doReturn(true).when(service).save(any(RepairRecord.class));
        doReturn(0L).when(service).count(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        when(repairLogMapper.insert(any(RepairLog.class))).thenReturn(1);
        when(repairLogMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void createRepairValidatesUploadedImagesAndReturnsCompleteItem() {
        String imageUrl = "https://cdn.example.test/repair-images/damage.jpg";

        RepairDtos.RepairItem result = service.createRepair(
                "user-1",
                request(List.of(imageUrl))
        );

        verify(fileRecordService).validateFileOwnership(
                "user-1", imageUrl, "repair_image");
        assertThat(result.id()).isNotBlank();
        assertThat(result.imageUrls()).containsExactly(imageUrl);
        assertThat(result.status()).isEqualTo("submitted");

        ArgumentCaptor<RepairRecord> recordCaptor = ArgumentCaptor.forClass(RepairRecord.class);
        verify(service).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getImageUrls()).contains(imageUrl);
    }

    @Test
    void createRepairRejectsImagesThatDoNotBelongToCurrentUser() {
        String imageUrl = "mock://repair-image";
        doThrow(BusinessException.badRequest("图片无效或不属于当前用户"))
                .when(fileRecordService)
                .validateFileOwnership("user-1", imageUrl, "repair_image");

        assertThatThrownBy(() -> service.createRepair("user-1", request(List.of(imageUrl))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片无效");
    }

    private static RepairDtos.CreateRepairRequest request(List<String> imageUrls) {
        return new RepairDtos.CreateRepairRequest(
                "house-1",
                "测试房源",
                "1201",
                "plumbing",
                "厨房水管漏水，需要尽快维修",
                imageUrls,
                "王小明",
                "13800138000",
                LocalDateTime.of(2026, 8, 20, 10, 0)
        );
    }
}
