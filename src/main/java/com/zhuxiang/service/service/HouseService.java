package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.House;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;

import java.util.List;
import com.zhuxiang.service.dto.AdminHouseDtos;
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

    /**
     * 创建新房源，返回管理端视图。
     */
    AdminHouseDtos.AdminHouseView createHouse(
            AdminHouseDtos.CreateHouseRequest request,
            String operatorId
    );

    /**
     * 获取所有房源（含智能锁绑定信息）。
     */
    List<AdminHouseDtos.AdminHouseView> getAllHousesWithLockInfo();

    /**
     * 根据房源 ID 获取管理端房源详情（含图片和智能锁绑定信息）。
     */
    AdminHouseDtos.AdminHouseView getAdminHouseById(String houseId);

    /**
     * 发布房源（将草稿状态改为可租）。
     */
    AdminHouseDtos.AdminHouseView publishHouse(String houseId);

    /**
     * 下架房源（将可租或草稿状态改为下架）。
     */
    AdminHouseDtos.AdminHouseView offlineHouse(String houseId);

    /**
     * 重新上架房源（将下架状态恢复为可租）。
     */
    AdminHouseDtos.AdminHouseView onlineHouse(String houseId);

    /**
     * 修改房源信息，返回管理端视图。
     */
    AdminHouseDtos.AdminHouseView updateHouse(
            String houseId,
            AdminHouseDtos.UpdateHouseRequest request,
            String operatorId
    );
}
