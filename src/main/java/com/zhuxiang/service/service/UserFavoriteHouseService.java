package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.UserFavoriteHouse;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.HouseDtos;

/**
* @author king-wang
* @description 针对表【user_favorite_house(用户收藏房源表)】的数据库操作Service
* @createDate 2026-06-12 19:58:11
*/
public interface UserFavoriteHouseService extends IService<UserFavoriteHouse> {

    /**
     * 收藏指定房源。
     */
    HouseDtos.FavoriteResult favorite(String userId, String houseId);

    /**
     * 取消收藏指定房源。
     */
    HouseDtos.FavoriteResult unfavorite(String userId, String houseId);

    /**
     * 分页查询用户收藏的房源。
     */
    PageData<HouseDtos.HouseView> getFavoriteHouses(
            String userId,
            long page,
            long pageSize
    );
}
