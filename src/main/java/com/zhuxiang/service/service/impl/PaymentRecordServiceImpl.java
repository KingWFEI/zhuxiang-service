package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.PaymentDtos.PaymentDetail;
import com.zhuxiang.service.dto.PaymentDtos.PaymentItem;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.mapper.PaymentRecordMapper;
import com.zhuxiang.service.service.PaymentRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord>
        implements PaymentRecordService {

    private static final Map<String, String> TYPE_TEXT_MAP = Map.of(
            "rent", "租金",
            "deposit", "押金",
            "service_fee", "服务费",
            "refund", "退款"
    );

    private static final Map<String, String> STATUS_TEXT_MAP = Map.of(
            "pending", "待支付",
            "success", "支付成功",
            "paid", "支付成功",
            "failed", "支付失败",
            "refunded", "已退款"
    );

    private static final Map<String, String> METHOD_TEXT_MAP = Map.of(
            "wechat", "微信支付",
            "alipay", "支付宝",
            "mock", "模拟支付"
    );

    @Override
    public PageData<PaymentItem> listMyPayments(String userId, String status, String type, long page, long pageSize) {
        var query = Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getUserId, userId)
                .orderByDesc(PaymentRecord::getCreatedAt);

        if (status != null && !status.isBlank()) {
            query.eq(PaymentRecord::getStatus, status);
        }
        if (type != null && !type.isBlank()) {
            query.eq(PaymentRecord::getType, type);
        }

        var result = page(new Page<>(page, pageSize), query);
        List<PaymentItem> items = result.getRecords().stream()
                .map(this::toItem)
                .toList();

        return PageData.of(items, page, pageSize, result.getTotal());
    }

    @Override
    public PaymentDetail getPaymentDetail(String userId, String paymentId) {
        PaymentRecord record = getById(paymentId);
        if (record == null) {
            throw BusinessException.notFound("支付记录不存在");
        }
        if (!userId.equals(record.getUserId())) {
            throw BusinessException.forbidden("无权查看该支付记录");
        }
        return toDetail(record);
    }

    @Override
    public String generatePaymentNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = count(Wrappers.<PaymentRecord>lambdaQuery()
                .ge(PaymentRecord::getCreatedAt, LocalDate.now().atStartOfDay()));
        return "ZF" + date + String.format("%04d", count + 1);
    }

    private PaymentItem toItem(PaymentRecord r) {
        return new PaymentItem(
                r.getId(), r.getPaymentNo(), r.getBillId(), r.getLeaseId(),
                r.getHouseName(), r.getType(), typeText(r.getType()),
                r.getAmount(), r.getStatus(), statusText(r.getStatus()),
                r.getPaymentChannel(), methodText(r.getPaymentChannel()),
                r.getPaidAt(), r.getCreatedAt()
        );
    }

    private PaymentDetail toDetail(PaymentRecord r) {
        return new PaymentDetail(
                r.getId(), r.getPaymentNo(), r.getBillId(), r.getLeaseId(),
                r.getHouseName(), r.getType(), typeText(r.getType()),
                r.getAmount(), r.getStatus(), statusText(r.getStatus()),
                r.getPaymentChannel(), methodText(r.getPaymentChannel()),
                r.getChannelTradeNo(), r.getPaidAt(), r.getCreatedAt(),
                r.getRemark()
        );
    }

    private static String typeText(String type) {
        return TYPE_TEXT_MAP.getOrDefault(type, type);
    }

    private static String statusText(String status) {
        return STATUS_TEXT_MAP.getOrDefault(status, status);
    }

    private static String methodText(String channel) {
        return METHOD_TEXT_MAP.getOrDefault(channel, channel);
    }
}
