package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.entity.Community;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.LockDevice;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.service.LockDeviceService;
import com.zhuxiang.service.mapper.LeaseMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【lease(租约表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:39
*/
@Service
public class LeaseServiceImpl extends ServiceImpl<LeaseMapper, Lease>
    implements LeaseService{

    private final HouseService houseService;
    private final CommunityService communityService;
    private final LockDeviceService lockDeviceService;

    public LeaseServiceImpl(
            HouseService houseService,
            CommunityService communityService,
            LockDeviceService lockDeviceService
    ) {
        this.houseService = houseService;
        this.communityService = communityService;
        this.lockDeviceService = lockDeviceService;
    }

    /**
     * 查询用户当前租约及关联住房信息。
     */
    @Override
    public ProfileDtos.CurrentHome getCurrentHome(String userId) {
        Lease lease = getOne(
                Wrappers.<Lease>lambdaQuery()
                        .eq(Lease::getUserId, userId)
                        .in(Lease::getStatus, "active", "pending")
                        .orderByDesc(Lease::getCreatedAt)
                        .last("LIMIT 1"),
                false
        );
        if (lease == null) {
            return null;
        }
        House house = houseService.getById(lease.getHouseId());
        if (house == null) {
            return null;
        }
        Community community = communityService.getById(house.getCommunityId());
        LockDevice lock = lockDeviceService.getOne(
                Wrappers.<LockDevice>lambdaQuery()
                        .eq(LockDevice::getHouseId, house.getId())
                        .last("LIMIT 1"),
                false
        );
        return new ProfileDtos.CurrentHome(
                house.getId(),
                community == null ? "" : community.getName(),
                house.getBuilding(),
                house.getUnit(),
                house.getRoom(),
                lease.getId(),
                lease.getStatus(),
                lock == null ? null : lock.getId(),
                lock == null ? "unknown" : lock.getStatus()
        );
    }
}




