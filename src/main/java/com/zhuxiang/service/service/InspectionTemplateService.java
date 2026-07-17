package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.InspectionDtos;
import com.zhuxiang.service.entity.HouseInspectionTemplate;

/**
 * 房源退租验收模板服务。
 */
public interface InspectionTemplateService extends IService<HouseInspectionTemplate> {

    /** 查询房源验收模板。未配置时返回空 rooms 列表。 */
    InspectionDtos.TemplateResponse getTemplate(String houseId);

    /** 保存或更新房源验收模板，version 自动递增。 */
    InspectionDtos.TemplateResponse saveTemplate(String houseId, String operatorId, InspectionDtos.SaveTemplateRequest request);
}
