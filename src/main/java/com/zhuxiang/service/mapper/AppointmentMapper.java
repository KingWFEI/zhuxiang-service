package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.Appointment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author king-wang
* @description 针对表【appointment(预约看房表)】的数据库操作Mapper
* @createDate 2026-06-12 19:56:26
* @Entity com.zhuxiang.service.entity.Appointment
*/
public interface AppointmentMapper extends BaseMapper<Appointment> {

    int updateWithStatusAndVersion(
            @Param("appointment") Appointment appointment,
            @Param("expectedStatus") String expectedStatus,
            @Param("expectedVersion") Integer expectedVersion
    );
}




