package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.Appointment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.BookingDtos;

/**
* @author king-wang
* @description 针对表【appointment(预约看房表)】的数据库操作Service
* @createDate 2026-06-12 19:56:26
*/
public interface AppointmentService extends IService<Appointment> {

    /**
     * 创建预约看房记录。
     */
    BookingDtos.AppointmentResult createAppointment(
            String userId,
            BookingDtos.AppointmentRequest request
    );
}
