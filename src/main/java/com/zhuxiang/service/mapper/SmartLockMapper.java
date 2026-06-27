package com.zhuxiang.service.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.entity.SmartLock;

import java.util.Collection;
import java.util.List;

/**
 * 智能门锁绑定Mapper。
 */
public interface SmartLockMapper extends BaseMapper<SmartLock> {

    default SmartLock selectLatestByHouseId(String houseId) {
        return selectOne(
                Wrappers.<SmartLock>lambdaQuery()
                        .eq(SmartLock::getHouseId, houseId)
                        .orderByDesc(SmartLock::getUpdatedAt)
                        .last("LIMIT 1")
        );
    }

    default List<SmartLock> selectLatestByHouseIds(Collection<String> houseIds) {
        if (houseIds == null || houseIds.isEmpty()) {
            return List.of();
        }
        return selectList(
                Wrappers.<SmartLock>lambdaQuery()
                        .in(SmartLock::getHouseId, houseIds)
                        .orderByDesc(SmartLock::getUpdatedAt)
        );
    }
}
