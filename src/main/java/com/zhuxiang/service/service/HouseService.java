package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.House;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.HouseDtos;

/**
* @author king-wang
* @description 针对表【house(房源主表)】的数据库操作Service
* @createDate 2026-06-12 19:57:05
*/
public interface HouseService extends IService<House> {

    /**
     * 分页获取首页房源流。
     */
    HouseDtos.FeedData getFeed(
            String category,
            long page,
            long pageSize,
            String userId
    );

    /**
     * 按筛选条件分页搜索房源。
     */
    PageData<HouseDtos.HouseView> searchHouses(
            String keyword,
            String category,
            String region,
            Integer minPrice,
            Integer maxPrice,
            String roomType,
            Integer minArea,
            Integer maxArea,
            String facilities,
            String sort,
            long page,
            long pageSize,
            String userId
    );

    /**
     * 获取指定房源详情。
     */
    HouseDtos.HouseDetail getHouseDetail(String houseId, String userId);

    /**
     * 获取房源筛选选项。
     */
    HouseDtos.FilterOptions getFilterOptions();

    /**
     * 获取可租用的房源实体。
     */
    House requireAvailableHouse(String houseId);

    /**
     * 将房源实体转换为列表视图。
     */
    HouseDtos.HouseView toHouseView(House house, String userId);
}
