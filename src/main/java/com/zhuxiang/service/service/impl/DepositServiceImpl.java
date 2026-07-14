package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.DepositDtos;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.DepositDeductionMapper;
import com.zhuxiang.service.mapper.DepositRecordMapper;
import com.zhuxiang.service.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DepositServiceImpl extends ServiceImpl<DepositRecordMapper, DepositRecord>
        implements DepositService {

    private static final Logger log = LoggerFactory.getLogger(DepositServiceImpl.class);

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "HOUSEKEEPER", "LANDLORD");

    private final DepositDeductionMapper deductionMapper;
    private final PaymentRecordService paymentRecordService;
    private final AlipayService alipayService;
    private final UserService userService;
    private final HouseService houseService;

    public DepositServiceImpl(
            DepositDeductionMapper deductionMapper,
            PaymentRecordService paymentRecordService,
            AlipayService alipayService,
            UserService userService,
            HouseService houseService
    ) {
        this.deductionMapper = deductionMapper;
        this.paymentRecordService = paymentRecordService;
        this.alipayService = alipayService;
        this.userService = userService;
        this.houseService = houseService;
    }

    @Override
    public DepositRecord createDeposit(DepositRecord record) {
        record.setId(UUID.randomUUID().toString());
        record.setWithheldAmount(0);
        record.setRefundedAmount(0);
        record.setStatus("held");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        save(record);
        return record;
    }

    @Override
    public DepositRecord getByLeaseId(String leaseId) {
        return getOne(
                Wrappers.<DepositRecord>lambdaQuery()
                        .eq(DepositRecord::getLeaseId, leaseId)
                        .last("LIMIT 1"),
                false
        );
    }

    @Override
    public List<DepositDeduction> getDeductions(String depositRecordId) {
        return deductionMapper.selectList(
                Wrappers.<DepositDeduction>lambdaQuery()
                        .eq(DepositDeduction::getDepositRecordId, depositRecordId)
                        .orderByAsc(DepositDeduction::getCreatedAt)
        );
    }

    @Override
    @Transactional
    public void settle(String depositRecordId, List<DepositDeduction> deductions, String settlementDetailJson) {
        DepositRecord record = getById(depositRecordId);
        if (record == null) {
            throw BusinessException.notFound("押金记录不存在");
        }
        if (!"held".equals(record.getStatus())) {
            throw BusinessException.badRequest("当前押金状态不允许结算");
        }

        int totalDeduction = 0;
        LocalDateTime now = LocalDateTime.now();
        for (DepositDeduction deduction : deductions) {
            deduction.setId(UUID.randomUUID().toString());
            deduction.setDepositRecordId(depositRecordId);
            deduction.setCreatedAt(now);
            deductionMapper.insert(deduction);
            totalDeduction += deduction.getAmount() != null ? deduction.getAmount() : 0;
        }

        record.setWithheldAmount(totalDeduction);
        record.setSettlementDetail(settlementDetailJson);
        record.setStatus("deducted");
        record.setUpdatedAt(now);
        updateById(record);
    }

    @Override
    @Transactional
    public void refund(String depositRecordId) {
        DepositRecord record = getById(depositRecordId);
        if (record == null) {
            throw BusinessException.notFound("押金记录不存在");
        }
        if (!"deducted".equals(record.getStatus())) {
            throw BusinessException.badRequest("当前押金状态不允许退款");
        }

        int withheld = record.getWithheldAmount() != null ? record.getWithheldAmount() : 0;
        int refundAmount = record.getAmount() - withheld;
        if (refundAmount <= 0) {
            // 全部扣完，无需退款，直接标记为已退款
            record.setStatus("refunded");
            record.setRefundedAmount(0);
            record.setRefundedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            updateById(record);
            return;
        }

        record.setStatus("refunding");
        record.setUpdatedAt(LocalDateTime.now());
        updateById(record);

        if (record.getPaymentRecordId() != null) {
            PaymentRecord originalPayment = paymentRecordService.getById(record.getPaymentRecordId());
            if (originalPayment != null && originalPayment.getChannelTradeNo() != null) {
                String outRequestNo = "RFD" + record.getId().replace("-", "").substring(0, 24);
                try {
                    AlipayService.AlipayRefundResult result = alipayService.refund(
                            originalPayment.getPaymentNo(),
                            String.valueOf(refundAmount),
                            outRequestNo
                    );
                    if (result != null) {
                        record.setRefundTradeNo(result.tradeNo());
                        record.setRefundChannel("alipay");
                    }
                } catch (Exception e) {
                    log.error("支付宝退款调用异常 depositRecordId={}", depositRecordId, e);
                    // 退款失败仍保留 refunding 状态，等待人工处理
                    return;
                }
            }
        }

        // 创建退款支付记录
        PaymentRecord refundRecord = new PaymentRecord();
        refundRecord.setId(UUID.randomUUID().toString());
        refundRecord.setPaymentNo(paymentRecordService.generatePaymentNo());
        refundRecord.setUserId(record.getUserId());
        refundRecord.setLeaseId(record.getLeaseId());
        refundRecord.setHouseId(record.getHouseId());
        refundRecord.setAmount(refundAmount);
        refundRecord.setPaymentChannel(record.getRefundChannel() != null ? record.getRefundChannel() : "alipay");
        refundRecord.setStatus("success");
        refundRecord.setType("refund");
        refundRecord.setRemark("押金退款");
        refundRecord.setRefundToRecordId(record.getPaymentRecordId());
        refundRecord.setPaidAt(LocalDateTime.now());
        refundRecord.setCreatedAt(LocalDateTime.now());
        refundRecord.setUpdatedAt(LocalDateTime.now());
        paymentRecordService.save(refundRecord);

        record.setRefundPaymentRecordId(refundRecord.getId());
        record.setRefundedAmount(refundAmount);
        record.setStatus("refunded");
        record.setRefundedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        updateById(record);
    }

    @Override
    public PageData<DepositDtos.AdminDepositItem> getDeposits(String operatorId, String status, String keyword, long page, long pageSize) {
        requireAdminRole(operatorId);

        LambdaQueryWrapper<DepositRecord> query = Wrappers.<DepositRecord>lambdaQuery()
                .eq(StringUtils.hasText(status), DepositRecord::getStatus, status)
                .orderByDesc(DepositRecord::getCreatedAt);

        if (StringUtils.hasText(keyword)) {
            List<String> userIds = findUserIds(keyword);
            List<String> houseIds = findHouseIds(keyword);
            query.and(w -> {
                w.like(DepositRecord::getId, keyword);
                if (!userIds.isEmpty()) w.or().in(DepositRecord::getUserId, userIds);
                if (!houseIds.isEmpty()) w.or().in(DepositRecord::getHouseId, houseIds);
            });
        }

        IPage<DepositRecord> result = page(new Page<>(page, pageSize), query);
        Map<String, User> tenants = loadTenants(result.getRecords());
        Map<String, House> houses = loadHouses(result.getRecords());

        List<DepositDtos.AdminDepositItem> items = result.getRecords().stream()
                .map(r -> {
                    User tenant = tenants.get(r.getUserId());
                    House house = houses.get(r.getHouseId());
                    return new DepositDtos.AdminDepositItem(
                            r.getId(), r.getLeaseId(),
                            tenant != null ? tenant.getNickname() : null,
                            tenant != null ? tenant.getPhone() : null,
                            house != null ? house.getTitle() : null,
                            r.getAmount(), r.getWithheldAmount(), r.getRefundedAmount(),
                            r.getStatus(), r.getCreatedAt()
                    );
                })
                .toList();
        return PageData.of(items, page, pageSize, result.getTotal());
    }

    private void requireAdminRole(String operatorId) {
        User user = userService.requireActiveUser(operatorId);
        if (!ADMIN_ROLES.contains(user.getRole())) {
            throw BusinessException.forbidden("当前账号无权查看管理端押金");
        }
    }

    private List<String> findUserIds(String keyword) {
        return userService.list(
                Wrappers.<User>lambdaQuery()
                        .select(User::getId)
                        .and(w -> w.like(User::getNickname, keyword).or().like(User::getPhone, keyword))
        ).stream().map(User::getId).toList();
    }

    private List<String> findHouseIds(String keyword) {
        return houseService.list(
                Wrappers.<House>lambdaQuery()
                        .select(House::getId)
                        .and(w -> w.like(House::getTitle, keyword)
                                .or().like(House::getAddress, keyword)
                                .or().like(House::getBuilding, keyword))
        ).stream().map(House::getId).toList();
    }

    private Map<String, User> loadTenants(List<DepositRecord> records) {
        Set<String> ids = records.stream().map(DepositRecord::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Collections.emptyMap() : userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<String, House> loadHouses(List<DepositRecord> records) {
        Set<String> ids = records.stream().map(DepositRecord::getHouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        return ids.isEmpty() ? Collections.emptyMap() : houseService.listByIds(ids).stream()
                .collect(Collectors.toMap(House::getId, Function.identity()));
    }
}
