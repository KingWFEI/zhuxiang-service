package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.DepositDtos;
import com.zhuxiang.service.entity.DepositDeduction;
import com.zhuxiang.service.entity.DepositRecord;

import java.util.List;

public interface DepositService extends IService<DepositRecord> {

    /** 签约时创建押金记录，status=held */
    DepositRecord createDeposit(DepositRecord record);

    /** 按租约ID查询押金记录 */
    DepositRecord getByLeaseId(String leaseId);

    /** 查询押金记录的扣款明细列表 */
    List<DepositDeduction> getDeductions(String depositRecordId);

    /** 退租结算：写入扣款明细，更新押金记录 withheld_amount/status */
    void settle(String depositRecordId, List<DepositDeduction> deductions, String settlementDetailJson);

    /** 执行押金退款（调用支付宝原路退回） */
    void refund(String depositRecordId);

    /** 管理端分页查询押金记录 */
    PageData<DepositDtos.AdminDepositItem> getDeposits(String operatorId, String status, String keyword, long page, long pageSize);
}
