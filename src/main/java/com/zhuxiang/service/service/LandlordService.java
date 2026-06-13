package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.Landlord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.HouseDtos;

/**
* @author king-wang
* @description 针对表【landlord(房东与平台管家资料表)】的数据库操作Service
* @createDate 2026-06-12 19:57:34
*/
public interface LandlordService extends IService<Landlord> {

    /**
     * 获取指定房东资料。
     */
    HouseDtos.LandlordView getLandlordDetail(String landlordId);

    /**
     * 获取存在的房东实体。
     */
    Landlord requireLandlord(String landlordId);
}
