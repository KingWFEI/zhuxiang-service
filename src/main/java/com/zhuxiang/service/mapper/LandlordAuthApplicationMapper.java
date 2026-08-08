package com.zhuxiang.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.entity.LandlordAuthApplication;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface LandlordAuthApplicationMapper extends BaseMapper<LandlordAuthApplication> {
    @Update("""
            UPDATE landlord_auth_application
            SET status = #{status}, reject_reason = #{rejectReason},
                reviewer_id = #{reviewerId}, reviewed_at = #{reviewedAt},
                updated_at = #{reviewedAt}
            WHERE id = #{applicationId} AND status = 'PENDING'
            """)
    int reviewPending(
            @Param("applicationId") String applicationId,
            @Param("status") String status,
            @Param("rejectReason") String rejectReason,
            @Param("reviewerId") String reviewerId,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );
}
