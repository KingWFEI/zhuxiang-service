package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.*;
import com.zhuxiang.service.entity.RentOrder;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RentOrderService extends IService<RentOrder> {

    RentOrderResponse createOrder(String userId, CreateRentOrderRequest request);

    PageData<RentOrderResponse> listMyOrders(String userId, long page, long pageSize);

    RentOrderResponse getOrderDetail(String userId, String orderId);

    RentOrderResponse submitRealName(String userId, String orderId, RealNameRequest request);

    ContractPreviewResponse getContractPreview(String userId, String orderId);

    RentOrderResponse confirmContract(String userId, String orderId);

    PaymentInfoResponse getPaymentInfo(String userId, String orderId);

    RentOrderResponse pay(String userId, String orderId, PayRequest request);

    void confirmPayment(String recordId, String channelTradeNo);

    RentOrderResponse sign(String userId, String orderId);

    RentOrderResponse cancelOrder(String userId, String orderId);

    void hideOrder(String userId, String orderId);
}
