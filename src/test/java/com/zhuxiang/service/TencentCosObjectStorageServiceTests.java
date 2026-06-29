package com.zhuxiang.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.zhuxiang.service.config.ObjectStorageProperties;
import com.zhuxiang.service.service.impl.TencentCosObjectStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TencentCosObjectStorageServiceTests {

    @Test
    void uploadsStreamWithMetadataAndBuildsConfiguredPublicUrl() {
        COSClient cosClient = mock(COSClient.class);
        when(cosClient.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());
        ObjectStorageProperties storageProperties = new ObjectStorageProperties();
        ObjectStorageProperties.Cos cos = storageProperties.getCos();
        cos.setBucket("bucket-1250000000");
        cos.setRegion("ap-guangzhou");
        cos.setKeyPrefix("/zhuxiang/");
        cos.setPublicBaseUrl("https://cdn.example.com/");
        TencentCosObjectStorageService service =
                new TencentCosObjectStorageService(cosClient, storageProperties);

        String url = service.store(
                "id-card/2026/06/29/image.webp",
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                3L,
                "image/webp"
        );

        assertThat(url).isEqualTo(
                "https://cdn.example.com/zhuxiang/id-card/2026/06/29/image.webp"
        );
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(cosClient).putObject(requestCaptor.capture());
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.getBucketName()).isEqualTo("bucket-1250000000");
        assertThat(request.getKey()).isEqualTo("zhuxiang/id-card/2026/06/29/image.webp");
        assertThat(request.getMetadata().getContentLength()).isEqualTo(3L);
        assertThat(request.getMetadata().getContentType()).isEqualTo("image/webp");
    }
}
