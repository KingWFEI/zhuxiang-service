package com.zhuxiang.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.entity.PaymentRefund;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRefundMapper extends BaseMapper<PaymentRefund> {

    @Select("SELECT * FROM payment_refund WHERE id = #{id} FOR UPDATE")
    PaymentRefund selectByIdForUpdate(@Param("id") String id);

    @Select("""
            SELECT id FROM payment_refund
            WHERE status IN ('pending', 'processing')
              AND (next_retry_at IS NULL OR next_retry_at <= #{now})
            ORDER BY created_at ASC
            LIMIT #{limit}
            """)
    List<String> selectRetryableIds(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
