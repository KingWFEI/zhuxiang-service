package com.zhuxiang.service;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.HousePropertyCertificateDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HousePropertyCertificate;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.HousePropertyCertificateMapper;
import com.zhuxiang.service.service.PrivateObjectStorageService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.HousePropertyCertificateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HousePropertyCertificateServiceTests {

    private final HouseMapper houseMapper = mock(HouseMapper.class);
    private final HousePropertyCertificateMapper certificateMapper =
            mock(HousePropertyCertificateMapper.class);
    private final UserService userService = mock(UserService.class);
    private final PrivateObjectStorageService privateStorage =
            mock(PrivateObjectStorageService.class);
    private HousePropertyCertificateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HousePropertyCertificateServiceImpl(
                houseMapper, userService, privateStorage);
        ReflectionTestUtils.setField(service, "baseMapper", certificateMapper);
        when(houseMapper.updateById(any(House.class))).thenReturn(1);
        when(certificateMapper.updateById(any(HousePropertyCertificate.class))).thenReturn(1);
        when(certificateMapper.insert(any(HousePropertyCertificate.class))).thenReturn(1);
        when(certificateMapper.clearCurrent(any(String.class))).thenReturn(1);
    }

    @Test
    void landlordCannotSubmitWithoutPropertyCertificate() {
        mockLandlord("landlord-1");
        when(houseMapper.selectById("house-1")).thenReturn(house("draft"));
        when(certificateMapper.selectCurrent("house-1")).thenReturn(null);

        assertThatThrownBy(() -> service.submitForReview("house-1", "landlord-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(400);
                    assertThat(exception.getMessage()).contains("先上传房产证");
                });
    }

    @Test
    void pendingCertificateMovesHouseToPendingReview() {
        mockLandlord("landlord-1");
        House house = house("draft");
        HousePropertyCertificate certificate = certificate("pending");
        when(houseMapper.selectById("house-1")).thenReturn(house);
        when(certificateMapper.selectCurrent("house-1")).thenReturn(certificate);

        House result = service.submitForReview("house-1", "landlord-1");

        assertThat(result.getStatus()).isEqualTo("pendingReview");
        assertThat(certificate.getSubmittedAt()).isNotNull();
        verify(houseMapper).updateById(house);
        verify(certificateMapper).updateById(certificate);
    }

    @Test
    void approvedCertificateCreatesNewHistoryRecordWhenHouseIsResubmitted() {
        mockLandlord("landlord-1");
        House house = house("draft");
        when(houseMapper.selectById("house-1")).thenReturn(house);
        when(certificateMapper.selectCurrent("house-1"))
                .thenReturn(certificate("approved"));

        service.submitForReview("house-1", "landlord-1");

        ArgumentCaptor<HousePropertyCertificate> captor =
                ArgumentCaptor.forClass(HousePropertyCertificate.class);
        verify(certificateMapper).clearCurrent("house-1");
        verify(certificateMapper).insert(captor.capture());
        assertThat(captor.getValue().getAuditStatus()).isEqualTo("pending");
        assertThat(captor.getValue().getIsCurrent()).isEqualTo(1);
        assertThat(captor.getValue().getSubmittedAt()).isNotNull();
        assertThat(house.getStatus()).isEqualTo("pendingReview");
    }

    @Test
    void offlineHouseCanBeResubmittedButDoesNotBecomeAvailableDirectly() {
        mockLandlord("landlord-1");
        House house = house("offline");
        when(houseMapper.selectById("house-1")).thenReturn(house);
        when(certificateMapper.selectCurrent("house-1"))
                .thenReturn(certificate("approved"));

        House result = service.submitForReview("house-1", "landlord-1");

        assertThat(result.getStatus()).isEqualTo("pendingReview");
        verify(houseMapper).updateById(house);
        verify(certificateMapper).clearCurrent("house-1");
        verify(certificateMapper).insert(any(HousePropertyCertificate.class));
    }

    @Test
    void adminApprovalPublishesHouseAndRecordsReviewer() {
        mockReviewer("admin-1");
        House house = house("pendingReview");
        HousePropertyCertificate certificate = certificate("pending");
        when(houseMapper.selectById("house-1")).thenReturn(house);
        when(certificateMapper.selectCurrent("house-1")).thenReturn(certificate);

        House result = service.review(
                "house-1",
                new HousePropertyCertificateDtos.ReviewRequest("APPROVE", null),
                "admin-1"
        );

        assertThat(result.getStatus()).isEqualTo("available");
        assertThat(certificate.getAuditStatus()).isEqualTo("approved");
        assertThat(certificate.getReviewerId()).isEqualTo("admin-1");
        assertThat(certificate.getReviewedAt()).isNotNull();
    }

    @Test
    void rejectionRequiresRemark() {
        mockReviewer("admin-1");
        when(houseMapper.selectById("house-1")).thenReturn(house("pendingReview"));
        when(certificateMapper.selectCurrent("house-1"))
                .thenReturn(certificate("pending"));

        assertThatThrownBy(() -> service.review(
                "house-1",
                new HousePropertyCertificateDtos.ReviewRequest("REJECT", " "),
                "admin-1"
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getMessage()).contains("必须填写审核意见"));
    }

    @Test
    void uploadCreatesNewCurrentHistoryRecordInPrivateStorage() {
        mockLandlord("landlord-1");
        when(houseMapper.selectById("house-1")).thenReturn(house("draft"));
        when(privateStorage.storePrivate(
                any(String.class), any(), org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq("image/jpeg")))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.jpg", "image/jpeg", new byte[]{1, 2, 3});

        HousePropertyCertificateDtos.CertificateView result =
                service.uploadForLandlord("house-1", "landlord-1", file);

        assertThat(result.originalName()).isEqualTo("certificate.jpg");
        assertThat(result.auditStatus()).isEqualTo("pending");
        assertThat(result.current()).isTrue();
        verify(certificateMapper).insert(any(HousePropertyCertificate.class));
    }

    private House house(String status) {
        House house = new House();
        house.setId("house-1");
        house.setLandlordId("landlord-1");
        house.setStatus(status);
        return house;
    }

    private HousePropertyCertificate certificate(String status) {
        HousePropertyCertificate certificate = new HousePropertyCertificate();
        certificate.setId("certificate-1");
        certificate.setHouseId("house-1");
        certificate.setLandlordId("landlord-1");
        certificate.setAuditStatus(status);
        certificate.setIsCurrent(1);
        return certificate;
    }

    private void mockLandlord(String id) {
        User user = new User();
        user.setId(id);
        user.setRole("LANDLORD");
        when(userService.requireActiveUser(id)).thenReturn(user);
    }

    private void mockReviewer(String id) {
        User user = new User();
        user.setId(id);
        user.setRole("ADMIN");
        when(userService.requireActiveUser(id)).thenReturn(user);
    }
}
