package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.HousePropertyCertificateDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HousePropertyCertificate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface HousePropertyCertificateService extends IService<HousePropertyCertificate> {

    HousePropertyCertificateDtos.CertificateView uploadForLandlord(
            String houseId, String landlordId, MultipartFile file);

    List<HousePropertyCertificateDtos.CertificateView> listForLandlord(
            String houseId, String landlordId);

    List<HousePropertyCertificateDtos.CertificateView> listForAdmin(
            String houseId, String operatorId);

    HousePropertyCertificateDtos.CertificateView getCurrentView(String houseId);

    House submitForReview(String houseId, String landlordId);

    House review(
            String houseId,
            HousePropertyCertificateDtos.ReviewRequest request,
            String operatorId
    );

    CertificateDownload openForLandlord(
            String houseId, String certificateId, String landlordId);

    CertificateDownload openForAdmin(
            String houseId, String certificateId, String operatorId);

    record CertificateDownload(
            String originalName,
            String contentType,
            long contentLength,
            InputStream inputStream
    ) {
    }
}
