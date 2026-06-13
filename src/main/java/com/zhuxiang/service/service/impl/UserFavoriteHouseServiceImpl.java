package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.UserFavoriteHouse;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserFavoriteHouseService;
import com.zhuxiang.service.mapper.UserFavoriteHouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
* @author king-wang
* @description 针对表【user_favorite_house(用户收藏房源表)】的数据库操作Service实现
* @createDate 2026-06-12 19:58:11
*/
@Service
public class UserFavoriteHouseServiceImpl extends ServiceImpl<UserFavoriteHouseMapper, UserFavoriteHouse>
    implements UserFavoriteHouseService{

    private final HouseService houseService;

    public UserFavoriteHouseServiceImpl(HouseService houseService) {
        this.houseService = houseService;
    }

    /**
     * 创建用户房源收藏关系。
     */
    @Override
    @Transactional
    public HouseDtos.FavoriteResult favorite(String userId, String houseId) {
        House house = houseService.requireAvailableHouse(houseId);
        if (count(Wrappers.<UserFavoriteHouse>lambdaQuery()
                .eq(UserFavoriteHouse::getUserId, userId)
                .eq(UserFavoriteHouse::getHouseId, houseId)) == 0) {
            UserFavoriteHouse favorite = new UserFavoriteHouse();
            favorite.setId(UUID.randomUUID().toString());
            favorite.setUserId(userId);
            favorite.setHouseId(houseId);
            favorite.setCreatedAt(LocalDateTime.now());
            save(favorite);
            house.setFavoriteCount((house.getFavoriteCount() == null ? 0 : house.getFavoriteCount()) + 1);
            houseService.updateById(house);
        }
        return new HouseDtos.FavoriteResult(houseId, true);
    }

    /**
     * 删除用户房源收藏关系。
     */
    @Override
    @Transactional
    public HouseDtos.FavoriteResult unfavorite(String userId, String houseId) {
        House house = houseService.requireAvailableHouse(houseId);
        boolean removed = remove(
                Wrappers.<UserFavoriteHouse>lambdaQuery()
                        .eq(UserFavoriteHouse::getUserId, userId)
                        .eq(UserFavoriteHouse::getHouseId, houseId)
        );
        if (removed) {
            house.setFavoriteCount(Math.max(
                    0,
                    (house.getFavoriteCount() == null ? 0 : house.getFavoriteCount()) - 1
            ));
            houseService.updateById(house);
        }
        return new HouseDtos.FavoriteResult(houseId, false);
    }

    /**
     * 分页查询并组装用户收藏房源。
     */
    @Override
    public PageData<HouseDtos.HouseView> getFavoriteHouses(
            String userId,
            long page,
            long pageSize
    ) {
        IPage<UserFavoriteHouse> favorites = page(
                new Page<>(page, pageSize),
                Wrappers.<UserFavoriteHouse>lambdaQuery()
                        .eq(UserFavoriteHouse::getUserId, userId)
                        .orderByDesc(UserFavoriteHouse::getCreatedAt)
        );
        List<String> houseIds = favorites.getRecords().stream()
                .map(UserFavoriteHouse::getHouseId)
                .toList();
        if (houseIds.isEmpty()) {
            return PageData.of(List.of(), page, pageSize, favorites.getTotal());
        }
        List<House> houses = houseService.listByIds(houseIds);
        List<HouseDtos.HouseView> items = houseIds.stream()
                .map(id -> houses.stream()
                        .filter(house -> house.getId().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(house -> houseService.toHouseView(house, userId))
                .toList();
        return PageData.of(items, page, pageSize, favorites.getTotal());
    }
}




