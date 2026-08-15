package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminBillDtos;

import java.time.LocalDate;

public interface AdminBillService {

    PageData<AdminBillDtos.BillView> getBills(
            String operatorId,
            String status,
            String keyword,
            LocalDate dueDateStart,
            LocalDate dueDateEnd,
            long page,
            long pageSize
    );

    AdminBillDtos.BillView getBill(String operatorId, String billId);

    AdminBillDtos.BillSummary getSummary(String operatorId);
}
