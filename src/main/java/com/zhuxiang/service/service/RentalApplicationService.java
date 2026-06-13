package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.RentalApplication;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.BookingDtos;

/**
* @author king-wang
* @description 针对表【rental_application(租住申请表)】的数据库操作Service
* @createDate 2026-06-12 19:58:03
*/
public interface RentalApplicationService extends IService<RentalApplication> {

    /**
     * 创建房源租住申请。
     */
    BookingDtos.RentalApplicationResult createRentalApplication(
            String userId,
            BookingDtos.RentalApplicationRequest request
    );
}
