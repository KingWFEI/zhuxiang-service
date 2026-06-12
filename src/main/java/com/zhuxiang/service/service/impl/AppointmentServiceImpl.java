package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.Appointment;
import com.zhuxiang.service.service.AppointmentService;
import com.zhuxiang.service.mapper.AppointmentMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【appointment(预约看房表)】的数据库操作Service实现
* @createDate 2026-06-12 19:56:26
*/
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment>
    implements AppointmentService{

}




