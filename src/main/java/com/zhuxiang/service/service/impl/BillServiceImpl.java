package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.BillDtos;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.RentBillMapper;
import com.zhuxiang.service.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillServiceImpl extends ServiceImpl<RentBillMapper, RentBill>
        implements BillService {

    private static final Logger log = LoggerFactory.getLogger(BillServiceImpl.class);

    /** 滞纳金日费率：0.05%（万分之五） */
    private static final double OVERDUE_DAILY_RATE = 0.0005;

    private final LeaseService leaseService;
    private final HouseService houseService;
    private final PaymentRecordService paymentRecordService;
    private final AlipayService alipayService;

    public BillServiceImpl(
            LeaseService leaseService,
            HouseService houseService,
            PaymentRecordService paymentRecordService,
            AlipayService alipayService
    ) {
        this.leaseService = leaseService;
        this.houseService = houseService;
        this.paymentRecordService = paymentRecordService;
        this.alipayService = alipayService;
    }

    @Override
    public BillDtos.BillGroupedResponse getMyBills(String userId) {
        List<Lease> leases = leaseService.list(
                Wrappers.<Lease>lambdaQuery()
                        .eq(Lease::getUserId, userId)
                        .in(Lease::getStatus, "active", "pending")
        );

        Set<String> leaseIds = leases.stream().map(Lease::getId).collect(Collectors.toSet());
        if (leaseIds.isEmpty()) {
            return new BillDtos.BillGroupedResponse(List.of(), List.of(), List.of());
        }

        List<RentBill> allBills = list(
                Wrappers.<RentBill>lambdaQuery()
                        .in(RentBill::getLeaseId, leaseIds)
                        .ne(RentBill::getStatus, "cancelled")
                        .orderByAsc(RentBill::getPeriodNo)
        );

        Map<String, House> houseCache = new HashMap<>();
        Map<String, Lease> leaseCache = leases.stream()
                .collect(Collectors.toMap(Lease::getId, l -> l));

        List<BillDtos.BillItem> scheduledBills = new ArrayList<>();
        List<BillDtos.BillItem> pendingBills = new ArrayList<>();
        List<BillDtos.BillItem> paidBills = new ArrayList<>();

        for (RentBill bill : allBills) {
            Lease lease = leaseCache.get(bill.getLeaseId());
            if (lease == null) continue;

            House house = houseCache.computeIfAbsent(lease.getHouseId(),
                    houseService::getById);

            BillDtos.BillItem item = toItem(bill, lease, house);
            switch (bill.getStatus()) {
                case "paid":
                    paidBills.add(item);
                    break;
                case "scheduled":
                    scheduledBills.add(item);
                    break;
                default:
                    pendingBills.add(item);
                    break;
            }
        }

        // 未到期按 due_date 升序
        scheduledBills.sort(Comparator.comparing(BillDtos.BillItem::dueDate));
        // 待付按 due_date 升序
        pendingBills.sort(Comparator.comparing(BillDtos.BillItem::dueDate));
        // 已付按 paid_at 降序
        paidBills.sort(Comparator.comparing(
                b -> b.paidAt() != null ? b.paidAt() : LocalDateTime.MIN,
                Comparator.reverseOrder()));

        return new BillDtos.BillGroupedResponse(scheduledBills, pendingBills, paidBills);
    }

    @Override
    public BillDtos.BillItem getBillDetail(String userId, String billId) {
        RentBill bill = getById(billId);
        if (bill == null) {
            throw BusinessException.notFound("账单不存在");
        }
        Lease lease = leaseService.getById(bill.getLeaseId());
        if (lease == null || !userId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权查看该账单");
        }
        House house = houseService.getById(lease.getHouseId());
        return toItem(bill, lease, house);
    }

    @Override
    @Transactional
    public BillDtos.BillPayResponse payBill(String userId, String billId, BillDtos.BillPayRequest request) {
        RentBill bill = getById(billId);
        if (bill == null) {
            throw BusinessException.notFound("账单不存在");
        }

        Lease lease = leaseService.getById(bill.getLeaseId());
        if (lease == null || !userId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权支付该账单");
        }

        if ("paid".equals(bill.getStatus())) {
            throw BusinessException.badRequest("该账单已支付");
        }
        if ("scheduled".equals(bill.getStatus())) {
            throw BusinessException.badRequest("该账单尚未到缴款日");
        }

        House house = houseService.getById(lease.getHouseId());
        String houseName = house != null ? house.getTitle() : "";

        int totalAmount = bill.getAmountDue() - bill.getAmountPaid()
                + (bill.getOverdueAmount() != null ? bill.getOverdueAmount() : 0);

        String channel = request.paymentChannel() != null && !request.paymentChannel().isBlank()
                ? request.paymentChannel() : "mock";

        PaymentRecord record = new PaymentRecord();
        record.setId(UUID.randomUUID().toString());
        record.setPaymentNo(paymentRecordService.generatePaymentNo());
        record.setBillId(billId);
        record.setLeaseId(lease.getId());
        record.setUserId(userId);
        record.setHouseId(lease.getHouseId());
        record.setHouseName(houseName);
        record.setType("rent");
        record.setAmount(totalAmount);
        record.setPaymentChannel(channel);
        record.setStatus("pending");
        record.setRemark("第" + bill.getPeriodNo() + "期租金"
                + (bill.getOverdueAmount() != null && bill.getOverdueAmount() > 0
                        ? "（含滞纳金" + bill.getOverdueAmount() / 100.0 + "元）" : ""));
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        paymentRecordService.save(record);

        String payType = null;
        String paymentUrl = null;

        if ("alipay".equals(channel)) {
            String subject = "勿忧管家租房-第" + bill.getPeriodNo() + "期租金-" + houseName;
            try {
                paymentUrl = alipayService.buildH5PayUrl(record.getPaymentNo(), totalAmount, subject);
                payType = alipayService.getPayType();
            } catch (Exception e) {
                log.error("支付宝下单失败 billId={}", billId, e);
            }
        } else if ("mock".equals(channel)) {
            confirmBillPayment(record.getId(), null);
        }

        return new BillDtos.BillPayResponse(
                billId, record.getId(), record.getPaymentNo(),
                payType, paymentUrl, totalAmount
        );
    }

    @Transactional
    public void confirmBillPayment(String recordId, String channelTradeNo) {
        PaymentRecord record = paymentRecordService.getById(recordId);
        if (record == null || !"pending".equals(record.getStatus())) {
            throw BusinessException.badRequest("支付记录不存在或状态不正确");
        }

        LocalDateTime now = LocalDateTime.now();
        String tradeNo = channelTradeNo != null ? channelTradeNo
                : "mock_" + UUID.randomUUID().toString().replace("-", "");

        record.setStatus("success");
        record.setChannelTradeNo(tradeNo);
        record.setPaidAt(now);
        record.setCallbackTime(now);
        record.setUpdatedAt(now);
        paymentRecordService.updateById(record);

        if (record.getBillId() != null) {
            RentBill bill = getById(record.getBillId());
            if (bill != null) {
                bill.setAmountPaid(bill.getAmountDue());
                bill.setOverdueAmount(0);
                bill.setPaidAt(now);
                bill.setStatus("paid");
                bill.setUpdatedAt(now);
                updateById(bill);
            }
        }
    }

    @Override
    public int markOverdueBills() {
        List<RentBill> pendingBills = list(
                Wrappers.<RentBill>lambdaQuery()
                        .eq(RentBill::getStatus, "pending")
                        .lt(RentBill::getDueDate, LocalDate.now())
        );

        int marked = 0;
        LocalDate today = LocalDate.now();

        for (RentBill bill : pendingBills) {
            long overdueDays = today.toEpochDay() - bill.getDueDate().toEpochDay();
            if (overdueDays <= 0) continue;

            int overdueAmount = (int) (bill.getAmountDue() * OVERDUE_DAILY_RATE * overdueDays);
            int maxOverdue = bill.getAmountDue() / 5;
            overdueAmount = Math.min(overdueAmount, maxOverdue);

            bill.setStatus("overdue");
            bill.setOverdueAmount(overdueAmount);
            bill.setUpdatedAt(LocalDateTime.now());
            updateById(bill);
            marked++;
        }

        if (marked > 0) {
            log.info("标记逾期账单 {} 条", marked);
        }
        return marked;
    }

    @Override
    public int activateScheduledBills() {
        List<RentBill> scheduled = list(
                Wrappers.<RentBill>lambdaQuery()
                        .eq(RentBill::getStatus, "scheduled")
                        .le(RentBill::getDueDate, LocalDate.now())
        );

        LocalDateTime now = LocalDateTime.now();
        for (RentBill bill : scheduled) {
            bill.setStatus("pending");
            bill.setUpdatedAt(now);
            updateById(bill);
        }

        if (!scheduled.isEmpty()) {
            log.info("激活到期账单 {} 条", scheduled.size());
        }
        return scheduled.size();
    }

    @Override
    public int cancelUnpaidBills(String leaseId) {
        LocalDateTime now = LocalDateTime.now();
        List<RentBill> bills = list(
                Wrappers.<RentBill>lambdaQuery()
                        .eq(RentBill::getLeaseId, leaseId)
                        .in(RentBill::getStatus, "scheduled", "pending", "overdue")
        );

        for (RentBill bill : bills) {
            bill.setStatus("cancelled");
            bill.setUpdatedAt(now);
            updateById(bill);
        }

        if (!bills.isEmpty()) {
            log.info("取消未付账单 {} 条 leaseId={}", bills.size(), leaseId);
        }
        return bills.size();
    }

    private BillDtos.BillItem toItem(RentBill bill, Lease lease, House house) {
        return new BillDtos.BillItem(
                bill.getId(),
                bill.getLeaseId(),
                house != null ? house.getTitle() : "",
                house != null ? (house.getCoverImage() != null ? house.getCoverImage() : "") : "",
                bill.getPeriodNo(),
                bill.getAmountDue(),
                bill.getAmountPaid(),
                bill.getOverdueAmount() != null ? bill.getOverdueAmount() : 0,
                bill.getDueDate(),
                bill.getPaidAt(),
                bill.getStatus()
        );
    }
}
