package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.RentOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface RentOrderMapper extends BaseMapper<RentOrder> {

    @Select("""
            SELECT ro.*
            FROM rent_order ro
            INNER JOIN rent_contract rc ON rc.order_id = ro.id
            WHERE ro.lessor_user_id = #{lessorUserId}
              AND ro.status = 'pendingLandlordSign'
              AND rc.lessor_signed = 0
              AND rc.status NOT IN ('signed', 'canceled', 'expired')
            ORDER BY COALESCE(rc.updated_at, ro.updated_at) DESC
            """)
    IPage<RentOrder> selectLandlordPendingSignPage(
            Page<RentOrder> page,
            @Param("lessorUserId") String lessorUserId);

    @Select("SELECT * FROM rent_order WHERE id = #{orderId} LIMIT 1 FOR UPDATE")
    RentOrder selectByIdForUpdate(@Param("orderId") String orderId);

    @Select("""
            SELECT id FROM rent_order
            WHERE status = 'pendingPayment'
              AND payment_deadline_at IS NOT NULL
              AND payment_deadline_at <= #{now}
            ORDER BY payment_deadline_at ASC
            LIMIT #{limit}
            """)
    List<String> selectExpiredPaymentOrderIds(@Param("now") LocalDateTime now,
                                               @Param("limit") int limit);

    @Select("""
            SELECT id FROM rent_order
            WHERE status IN ('pendingRealName', 'pendingContract', 'pendingTenantSign')
              AND paid_at IS NULL
              AND pre_payment_deadline_at IS NOT NULL
              AND pre_payment_deadline_at <= #{now}
            ORDER BY pre_payment_deadline_at ASC
            LIMIT #{limit}
            """)
    List<String> selectExpiredPrePaymentOrderIds(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);
}
