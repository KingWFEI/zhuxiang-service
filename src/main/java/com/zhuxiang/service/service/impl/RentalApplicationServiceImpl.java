package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.BookingDtos;
import com.zhuxiang.service.entity.AppUser;
import com.zhuxiang.service.entity.RentalApplication;
import com.zhuxiang.service.service.AppUserService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.RentalApplicationService;
import com.zhuxiang.service.mapper.RentalApplicationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
* @author king-wang
* @description 针对表【rental_application(租住申请表)】的数据库操作Service实现
* @createDate 2026-06-12 19:58:03
*/
@Service
public class RentalApplicationServiceImpl extends ServiceImpl<RentalApplicationMapper, RentalApplication>
    implements RentalApplicationService{

    private final AppUserService appUserService;
    private final HouseService houseService;

    public RentalApplicationServiceImpl(
            AppUserService appUserService,
            HouseService houseService
    ) {
        this.appUserService = appUserService;
        this.houseService = houseService;
    }

    /**
     * 校验用户与房源后创建租住申请。
     */
    @Override
    @Transactional
    public BookingDtos.RentalApplicationResult createRentalApplication(
            String userId,
            BookingDtos.RentalApplicationRequest request
    ) {
        AppUser user = appUserService.requireActiveUser(userId);
        if (!Integer.valueOf(1).equals(user.getIsVerified())) {
            throw BusinessException.forbidden("请先完成实名认证");
        }
        houseService.requireAvailableHouse(request.houseId());
        if (count(Wrappers.<RentalApplication>lambdaQuery()
                .eq(RentalApplication::getUserId, userId)
                .eq(RentalApplication::getHouseId, request.houseId())
                .eq(RentalApplication::getStatus, "pending")) > 0) {
            throw BusinessException.conflict("该房源已有待处理的租住申请");
        }
        RentalApplication application = new RentalApplication();
        application.setId(UUID.randomUUID().toString());
        application.setUserId(userId);
        application.setHouseId(request.houseId());
        application.setLeaseStartDate(request.leaseStartDate());
        application.setLeaseMonths(request.leaseMonths());
        application.setRemark(request.remark());
        application.setStatus("pending");
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        save(application);
        return new BookingDtos.RentalApplicationResult(
                application.getId(),
                application.getStatus()
        );
    }
}




