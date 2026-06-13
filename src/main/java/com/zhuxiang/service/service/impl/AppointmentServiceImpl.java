package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.dto.BookingDtos;
import com.zhuxiang.service.entity.Appointment;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.AppointmentService;
import com.zhuxiang.service.mapper.AppointmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
* @author king-wang
* @description 针对表【appointment(预约看房表)】的数据库操作Service实现
* @createDate 2026-06-12 19:56:26
*/
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment>
    implements AppointmentService{

    private final HouseService houseService;

    public AppointmentServiceImpl(HouseService houseService) {
        this.houseService = houseService;
    }

    /**
     * 校验房源并创建预约看房记录。
     */
    @Override
    @Transactional
    public BookingDtos.AppointmentResult createAppointment(
            String userId,
            BookingDtos.AppointmentRequest request
    ) {
        houseService.requireAvailableHouse(request.houseId());
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID().toString());
        appointment.setUserId(userId);
        appointment.setHouseId(request.houseId());
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setTimeSlot(request.timeSlot());
        appointment.setContactName(request.contactName());
        appointment.setContactPhone(request.contactPhone());
        appointment.setRemark(request.remark());
        appointment.setStatus("pending");
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        save(appointment);
        return new BookingDtos.AppointmentResult(
                appointment.getId(),
                appointment.getHouseId(),
                appointment.getStatus()
        );
    }
}




