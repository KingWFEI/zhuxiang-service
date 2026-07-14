package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.BillDtos;

import java.util.List;

public interface BillService {

    /** 获取当前用户所有账单，按状态分组 */
    BillDtos.BillGroupedResponse getMyBills(String userId);

    /** 获取单条账单详情（校验归属） */
    BillDtos.BillItem getBillDetail(String userId, String billId);

    /** 账单支付，创建支付记录并返回支付 URL */
    BillDtos.BillPayResponse payBill(String userId, String billId, BillDtos.BillPayRequest request);

    /** 扫描逾期账单并标记 */
    int markOverdueBills();

    /** 激活到期的 scheduled 账单（scheduled → pending） */
    int activateScheduledBills();

    /** 取消租约下所有未付账单（退租时调用） */
    int cancelUnpaidBills(String leaseId);
}
