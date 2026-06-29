package com.zhuxiang.service;

import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.entity.FileRecord;
import com.zhuxiang.service.mapper.FileRecordMapper;
import com.zhuxiang.service.service.ObjectStorageService;
import com.zhuxiang.service.service.impl.FileRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileRecordServiceTests {

    private final ObjectStorageService objectStorageService = mock(ObjectStorageService.class);
    private final FileRecordMapper fileRecordMapper = mock(FileRecordMapper.class);
    private FileRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileRecordServiceImpl(objectStorageService);
        ReflectionTestUtils.setField(service, "baseMapper", fileRecordMapper);
        when(fileRecordMapper.insert(any(FileRecord.class))).thenReturn(1);
    }

    @Test
    void uploadsValidatedImageToConfiguredStorageAndPersistsReturnedUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "identity.png", "image/png", new byte[]{1, 2, 3}
        );
        when(objectStorageService.store(
                any(String.class), any(InputStream.class), anyLong(), eq("image/png")
        )).thenReturn("https://cdn.example.com/zhuxiang/id-card/image.png");

        FileUploadResponse response = service.upload("user-1", file, "id_card_front");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorageService).store(
                keyCaptor.capture(), any(InputStream.class), eq(3L), eq("image/png")
        );
        assertThat(keyCaptor.getValue())
                .matches("id-card/\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]{36}\\.png");
        assertThat(response.url()).isEqualTo("https://cdn.example.com/zhuxiang/id-card/image.png");

        ArgumentCaptor<FileRecord> recordCaptor = ArgumentCaptor.forClass(FileRecord.class);
        verify(fileRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(recordCaptor.getValue().getBizType()).isEqualTo("id_card_front");
        assertThat(recordCaptor.getValue().getUrl()).isEqualTo(response.url());
    }

    @Test
    void uploadsHouseImageToDedicatedObjectDirectoryAndBusinessType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "room.jpg", "image/jpeg", new byte[]{1, 2}
        );
        when(objectStorageService.store(
                any(String.class), any(InputStream.class), anyLong(), eq("image/jpeg")
        )).thenReturn("https://cdn.example.com/zhuxiang/house-images/room.jpg");

        FileUploadResponse response = service.uploadHouseImage("admin-1", file);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorageService).store(
                keyCaptor.capture(), any(InputStream.class), eq(2L), eq("image/jpeg")
        );
        assertThat(keyCaptor.getValue())
                .matches("house-images/\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]{36}\\.jpg");
        assertThat(response.url()).contains("/house-images/");

        ArgumentCaptor<FileRecord> recordCaptor = ArgumentCaptor.forClass(FileRecord.class);
        verify(fileRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getUserId()).isEqualTo("admin-1");
        assertThat(recordCaptor.getValue().getBizType()).isEqualTo("house_image");
    }
}
