package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.HousePropertyCertificateDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HousePropertyCertificate;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.HousePropertyCertificateMapper;
import com.zhuxiang.service.service.HousePropertyCertificateService;
import com.zhuxiang.service.service.PrivateObjectStorageService;
import com.zhuxiang.service.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class HousePropertyCertificateServiceImpl
        extends ServiceImpl<HousePropertyCertificateMapper, HousePropertyCertificate>
        implements HousePropertyCertificateService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );
    private static final Set<String> REVIEW_ROLES = Set.of("ADMIN", "HOUSEKEEPER");
    private static final DateTimeFormatter PATH_DATE = DateTimeFormatter.ofPattern("yyyy/MM");

    private final HouseMapper houseMapper;
    private final UserService userService;
    private final PrivateObjectStorageService privateObjectStorageService;

    public HousePropertyCertificateServiceImpl(
            HouseMapper houseMapper,
            UserService userService,
            PrivateObjectStorageService privateObjectStorageService
    ) {
        this.houseMapper = houseMapper;
        this.userService = userService;
        this.privateObjectStorageService = privateObjectStorageService;
    }

    @Override
    @Transactional
    public HousePropertyCertificateDtos.CertificateView uploadForLandlord(
            String houseId, String landlordId, MultipartFile file) {
        requireLandlord(landlordId);
        House house = requireOwnedHouse(houseId, landlordId);
        if ("reserved".equals(house.getStatus()) || "rented".equals(house.getStatus())) {
            throw BusinessException.badRequest("已被预定或已出租的房源不能替换房产证");
        }
        validateFile(file);

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String objectKey = "property-certificates/" + landlordId + "/" + houseId + "/"
                + LocalDate.now().format(PATH_DATE) + "/" + UUID.randomUUID()
                + CONTENT_TYPE_EXTENSIONS.get(contentType);
        try (InputStream input = file.getInputStream()) {
            String storedObjectKey = privateObjectStorageService.storePrivate(
                    objectKey, input, file.getSize(), contentType);
            baseMapper.clearCurrent(houseId);

            LocalDateTime now = LocalDateTime.now();
            HousePropertyCertificate certificate = new HousePropertyCertificate();
            certificate.setId(UUID.randomUUID().toString());
            certificate.setHouseId(houseId);
            certificate.setLandlordId(landlordId);
            certificate.setOriginalName(normalizeOriginalName(file.getOriginalFilename()));
            certificate.setObjectKey(storedObjectKey);
            certificate.setContentType(contentType);
            certificate.setFileSize(file.getSize());
            certificate.setAuditStatus("pending");
            certificate.setIsCurrent(1);
            certificate.setCreatedAt(now);
            save(certificate);

            // 替换材料后必须重新提交审核；已公开房源会立即退出公开列表。
            if (!"draft".equals(house.getStatus())) {
                house.setStatus("draft");
                house.setUpdatedAt(now);
                houseMapper.updateById(house);
            }
            log.info(
                    "房产证上传留痕 houseId={}, certificateId={}, landlordId={}",
                    houseId, certificate.getId(), landlordId);
            return toView(certificate);
        } catch (IOException exception) {
            throw new IllegalStateException("房产证文件保存失败", exception);
        }
    }

    @Override
    public List<HousePropertyCertificateDtos.CertificateView> listForLandlord(
            String houseId, String landlordId) {
        requireLandlord(landlordId);
        requireOwnedHouse(houseId, landlordId);
        return listHistory(houseId);
    }

    @Override
    public List<HousePropertyCertificateDtos.CertificateView> listForAdmin(
            String houseId, String operatorId) {
        requireReviewer(operatorId);
        requireHouse(houseId);
        return listHistory(houseId);
    }

    @Override
    public HousePropertyCertificateDtos.CertificateView getCurrentView(String houseId) {
        HousePropertyCertificate certificate = currentCertificate(houseId);
        return certificate == null ? null : toView(certificate);
    }

    @Override
    @Transactional
    public House submitForReview(String houseId, String landlordId) {
        requireLandlord(landlordId);
        House house = requireOwnedHouse(houseId, landlordId);
        if (!"draft".equals(house.getStatus())
                && !"rejected".equals(house.getStatus())
                && !"offline".equals(house.getStatus())) {
            throw BusinessException.badRequest(
                    "只有草稿、已驳回或已下架状态的房源才能提交审核，当前状态：" + house.getStatus());
        }
        HousePropertyCertificate certificate = currentCertificate(houseId);
        if (certificate == null) {
            throw BusinessException.badRequest("请先上传房产证后再提交审核");
        }
        if ("rejected".equals(certificate.getAuditStatus())) {
            throw BusinessException.badRequest("房产证已被驳回，请重新上传后再提交审核");
        }
        if (!"pending".equals(certificate.getAuditStatus())
                && !"approved".equals(certificate.getAuditStatus())) {
            throw BusinessException.badRequest("当前房产证状态不能重新提交审核");
        }

        LocalDateTime now = LocalDateTime.now();
        if ("approved".equals(certificate.getAuditStatus())) {
            certificate = createReviewHistoryCopy(certificate, now);
        }
        certificate.setSubmittedAt(now);
        certificate.setReviewRemark(null);
        certificate.setReviewerId(null);
        certificate.setReviewedAt(null);
        updateById(certificate);

        house.setStatus("pendingReview");
        house.setUpdatedAt(now);
        houseMapper.updateById(house);
        log.info(
                "房源提交审核 houseId={}, certificateId={}, landlordId={}",
                houseId, certificate.getId(), landlordId);
        return house;
    }

    private HousePropertyCertificate createReviewHistoryCopy(
            HousePropertyCertificate source,
            LocalDateTime now
    ) {
        baseMapper.clearCurrent(source.getHouseId());
        HousePropertyCertificate copy = new HousePropertyCertificate();
        copy.setId(UUID.randomUUID().toString());
        copy.setHouseId(source.getHouseId());
        copy.setLandlordId(source.getLandlordId());
        copy.setOriginalName(source.getOriginalName());
        copy.setObjectKey(source.getObjectKey());
        copy.setContentType(source.getContentType());
        copy.setFileSize(source.getFileSize());
        copy.setAuditStatus("pending");
        copy.setIsCurrent(1);
        copy.setCreatedAt(now);
        save(copy);
        return copy;
    }

    @Override
    @Transactional
    public House review(
            String houseId,
            HousePropertyCertificateDtos.ReviewRequest request,
            String operatorId
    ) {
        requireReviewer(operatorId);
        House house = requireHouse(houseId);
        HousePropertyCertificate certificate = currentCertificate(houseId);
        if (certificate == null) {
            throw BusinessException.badRequest("该房源没有可审核的房产证");
        }
        String action = request.action().toUpperCase(Locale.ROOT);
        if ("APPROVE".equals(action)
                && "available".equals(house.getStatus())
                && "approved".equals(certificate.getAuditStatus())) {
            log.info("房源重复审核幂等 houseId={}, action=APPROVE, operatorId={}",
                    houseId, operatorId);
            return house;
        }
        if ("REJECT".equals(action)
                && "rejected".equals(house.getStatus())
                && "rejected".equals(certificate.getAuditStatus())) {
            log.info("房源重复审核幂等 houseId={}, action=REJECT, operatorId={}",
                    houseId, operatorId);
            return house;
        }
        if (!"pendingReview".equals(house.getStatus())
                || !"pending".equals(certificate.getAuditStatus())) {
            throw BusinessException.badRequest("房源当前不处于待审核状态");
        }
        if ("REJECT".equals(action) && !StringUtils.hasText(request.remark())) {
            throw BusinessException.badRequest("驳回时必须填写审核意见");
        }

        LocalDateTime now = LocalDateTime.now();
        certificate.setAuditStatus("APPROVE".equals(action) ? "approved" : "rejected");
        certificate.setReviewRemark(StringUtils.hasText(request.remark())
                ? request.remark().trim() : null);
        certificate.setReviewerId(operatorId);
        certificate.setReviewedAt(now);
        updateById(certificate);

        house.setStatus("APPROVE".equals(action) ? "available" : "rejected");
        house.setUpdatedAt(now);
        houseMapper.updateById(house);
        log.info(
                "房源审核留痕 houseId={}, certificateId={}, action={}, operatorId={}",
                houseId, certificate.getId(), action, operatorId);
        return house;
    }

    @Override
    public CertificateDownload openForLandlord(
            String houseId, String certificateId, String landlordId) {
        requireLandlord(landlordId);
        requireOwnedHouse(houseId, landlordId);
        return openCertificate(houseId, certificateId);
    }

    @Override
    public CertificateDownload openForAdmin(
            String houseId, String certificateId, String operatorId) {
        requireReviewer(operatorId);
        requireHouse(houseId);
        return openCertificate(houseId, certificateId);
    }

    private List<HousePropertyCertificateDtos.CertificateView> listHistory(String houseId) {
        return baseMapper.selectHistoryByHouseId(houseId)
                .stream()
                .map(this::toView)
                .toList();
    }

    private CertificateDownload openCertificate(String houseId, String certificateId) {
        HousePropertyCertificate certificate = getById(certificateId);
        if (certificate == null || !houseId.equals(certificate.getHouseId())) {
            throw BusinessException.notFound("房产证文件不存在");
        }
        PrivateObjectStorageService.StoredPrivateObject stored =
                privateObjectStorageService.openPrivate(certificate.getObjectKey());
        return new CertificateDownload(
                certificate.getOriginalName(),
                StringUtils.hasText(stored.contentType())
                        ? stored.contentType() : certificate.getContentType(),
                stored.contentLength() > 0 ? stored.contentLength() : certificate.getFileSize(),
                stored.inputStream()
        );
    }

    private HousePropertyCertificate currentCertificate(String houseId) {
        return baseMapper.selectCurrent(houseId);
    }

    private HousePropertyCertificateDtos.CertificateView toView(
            HousePropertyCertificate certificate) {
        return new HousePropertyCertificateDtos.CertificateView(
                certificate.getId(),
                certificate.getHouseId(),
                certificate.getOriginalName(),
                certificate.getContentType(),
                certificate.getFileSize(),
                certificate.getAuditStatus(),
                Integer.valueOf(1).equals(certificate.getIsCurrent()),
                certificate.getReviewRemark(),
                certificate.getReviewerId(),
                certificate.getSubmittedAt(),
                certificate.getReviewedAt(),
                certificate.getCreatedAt()
        );
    }

    private House requireHouse(String houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw BusinessException.notFound("房源不存在");
        }
        return house;
    }

    private House requireOwnedHouse(String houseId, String landlordId) {
        House house = requireHouse(houseId);
        if ("deleted".equals(house.getStatus())) {
            throw BusinessException.notFound("房源不存在");
        }
        if (!landlordId.equals(house.getLandlordId())) {
            throw BusinessException.forbidden("无权操作该房源的房产证");
        }
        return house;
    }

    private void requireLandlord(String userId) {
        User user = userService.requireActiveUser(userId);
        if (!"LANDLORD".equalsIgnoreCase(user.getRole())) {
            throw BusinessException.forbidden("仅房东可以上传房产证");
        }
    }

    private void requireReviewer(String userId) {
        User user = userService.requireActiveUser(userId);
        if (user.getRole() == null || !REVIEW_ROLES.contains(user.getRole().toUpperCase(Locale.ROOT))) {
            throw BusinessException.forbidden("仅管理员或管家可以审核房东房源");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("房产证文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("房产证文件大小不能超过10MB");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || !CONTENT_TYPE_EXTENSIONS.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            throw BusinessException.badRequest("房产证仅支持 JPG、PNG、WebP 图片");
        }
    }

    private String normalizeOriginalName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "property-certificate";
        }
        String normalized = originalName.replace('\\', '_').replace('/', '_').trim();
        return normalized.length() > 255 ? normalized.substring(normalized.length() - 255) : normalized;
    }
}
