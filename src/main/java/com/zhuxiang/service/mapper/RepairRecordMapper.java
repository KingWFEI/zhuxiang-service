package com.zhuxiang.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.entity.RepairRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface RepairRecordMapper extends BaseMapper<RepairRecord> {

    @Update("""
            UPDATE repair_record SET status = #{newStatus}, updated_at = #{updatedAt}
            WHERE id = #{id} AND status = #{expectedStatus} AND deleted_at IS NULL
            """)
    int updateStatusIfCurrent(
            @Param("id") String id,
            @Param("expectedStatus") String expectedStatus,
            @Param("newStatus") String newStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Update("""
            UPDATE repair_record
            SET status = 'assigned', assignee = #{assignee}, repairman_name = #{repairmanName},
                updated_at = #{updatedAt}
            WHERE id = #{id} AND status = 'accepted' AND deleted_at IS NULL
            """)
    int assignIfCurrent(
            @Param("id") String id,
            @Param("assignee") String assignee,
            @Param("repairmanName") String repairmanName,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Update("""
            UPDATE repair_record
            SET status = 'pendingReview', completed_time = #{completedAt}, updated_at = #{completedAt}
            WHERE id = #{id} AND status = 'processing' AND deleted_at IS NULL
            """)
    int finishIfProcessing(@Param("id") String id, @Param("completedAt") LocalDateTime completedAt);
}
