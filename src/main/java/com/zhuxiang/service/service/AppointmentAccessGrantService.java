package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.AppointmentDtos;
import com.zhuxiang.service.entity.AppointmentAccessGrant;

public interface AppointmentAccessGrantService {

    AppointmentAccessGrant grantForAppointment(String appointmentId);

    AppointmentDtos.AccessView getTenantAccess(String userId, String appointmentId);

    AppointmentAccessGrant retry(String appointmentId);

    void revoke(String appointmentId);

    void expire(String appointmentId);
}
