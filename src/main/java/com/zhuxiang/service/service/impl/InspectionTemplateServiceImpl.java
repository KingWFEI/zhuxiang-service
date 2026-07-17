package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.InspectionDtos;
import com.zhuxiang.service.entity.HouseInspectionTemplate;
import com.zhuxiang.service.mapper.HouseInspectionTemplateMapper;
import com.zhuxiang.service.service.InspectionTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 房源退租验收模板服务实现。
 */
@Service
public class InspectionTemplateServiceImpl
        extends ServiceImpl<HouseInspectionTemplateMapper, HouseInspectionTemplate>
        implements InspectionTemplateService {

    private static final Logger log = LoggerFactory.getLogger(InspectionTemplateServiceImpl.class);

    private final ObjectMapper objectMapper;

    public InspectionTemplateServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public InspectionDtos.TemplateResponse getTemplate(String houseId) {
        HouseInspectionTemplate template = getOne(
                Wrappers.<HouseInspectionTemplate>lambdaQuery()
                        .eq(HouseInspectionTemplate::getHouseId, houseId)
                        .last("LIMIT 1"), false);

        if (template == null) {
            return new InspectionDtos.TemplateResponse(houseId, 0,
                    Collections.emptyList(), null);
        }

        List<InspectionDtos.TemplateRoomItem> rooms = deserializeRooms(template.getRooms());
        return new InspectionDtos.TemplateResponse(
                template.getHouseId(), template.getVersion(),
                rooms, template.getUpdatedAt());
    }

    @Override
    @Transactional
    public InspectionDtos.TemplateResponse saveTemplate(String houseId, String operatorId,
                                                         InspectionDtos.SaveTemplateRequest request) {
        if (request.rooms() == null) {
            throw BusinessException.badRequest("验收标准 rooms 不能为空");
        }

        HouseInspectionTemplate existing = getOne(
                Wrappers.<HouseInspectionTemplate>lambdaQuery()
                        .eq(HouseInspectionTemplate::getHouseId, houseId)
                        .last("LIMIT 1"), false);

        String roomsJson;
        try {
            roomsJson = objectMapper.writeValueAsString(request.rooms());
        } catch (Exception e) {
            throw new RuntimeException("序列化验收模板失败", e);
        }

        LocalDateTime now = LocalDateTime.now();
        int newVersion;

        if (existing != null) {
            newVersion = (existing.getVersion() != null ? existing.getVersion() : 0) + 1;
            existing.setVersion(newVersion);
            existing.setRooms(roomsJson);
            existing.setUpdatedAt(now);
            updateById(existing);
        } else {
            newVersion = 1;
            HouseInspectionTemplate template = new HouseInspectionTemplate();
            template.setHouseId(houseId);
            template.setVersion(newVersion);
            template.setRooms(roomsJson);
            template.setCreatedAt(now);
            template.setUpdatedAt(now);
            save(template);
        }

        log.info("验收模板已保存: houseId={}, version={}, operatorId={}", houseId, newVersion, operatorId);
        return new InspectionDtos.TemplateResponse(houseId, newVersion, request.rooms(), now);
    }

    // ==================== JSON 序列化工具 ====================

    private List<InspectionDtos.TemplateRoomItem> deserializeRooms(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<InspectionDtos.TemplateRoomItem>>() {
                    });
        } catch (Exception e) {
            log.warn("反序列化验收模板 rooms 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
