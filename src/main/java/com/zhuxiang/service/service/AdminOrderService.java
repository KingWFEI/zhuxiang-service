package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminOrderDtos;

public interface AdminOrderService {
    PageData<AdminOrderDtos.OrderView> list(String operatorId, String status, String keyword,
                                            long page, long pageSize);
    AdminOrderDtos.OrderView get(String operatorId, String orderId);
}
