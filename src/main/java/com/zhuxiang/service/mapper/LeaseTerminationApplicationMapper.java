package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.LeaseTerminationApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface LeaseTerminationApplicationMapper extends BaseMapper<LeaseTerminationApplication> {

    @Select("SELECT * FROM lease_termination_applications WHERE id = #{id} FOR UPDATE")
    LeaseTerminationApplication selectByIdForUpdate(@Param("id") String id);

    @Select("SELECT * FROM lease_termination_applications WHERE rescission_sign_flow_id = #{signFlowId} LIMIT 1 FOR UPDATE")
    LeaseTerminationApplication selectByRescissionFlowIdForUpdate(@Param("signFlowId") String signFlowId);

    @Select("""
            SELECT id FROM lease_termination_applications
            WHERE status IN ('refund_pending', 'rescission_pending', 'rescission_signing')
              AND (process_retry_at IS NULL OR process_retry_at <= #{now})
            ORDER BY updated_at ASC
            LIMIT #{limit}
            """)
    List<String> selectProcessableIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Select("""
            SELECT a.* FROM lease_termination_applications a
            JOIN rent_contract c ON BINARY c.id = BINARY a.contract_id
            JOIN rent_order o ON BINARY o.id = BINARY c.order_id
            WHERE o.lessor_user_id = #{landlordUserId}
              AND a.deleted_at IS NULL
              AND a.status IN ('rescission_pending', 'rescission_signing')
            ORDER BY a.updated_at DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<LeaseTerminationApplication> selectLandlordPendingRescissions(
            @Param("landlordUserId") String landlordUserId,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize);

    @Select("""
            SELECT COUNT(*) FROM lease_termination_applications a
            JOIN rent_contract c ON BINARY c.id = BINARY a.contract_id
            JOIN rent_order o ON BINARY o.id = BINARY c.order_id
            WHERE o.lessor_user_id = #{landlordUserId}
              AND a.deleted_at IS NULL
              AND a.status IN ('rescission_pending', 'rescission_signing')
            """)
    long countLandlordPendingRescissions(@Param("landlordUserId") String landlordUserId);
}
