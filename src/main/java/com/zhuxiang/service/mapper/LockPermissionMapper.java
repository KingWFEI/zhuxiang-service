package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.LockPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @author king-wang
* @description 针对表【lock_permission(门锁权限表)】的数据库操作Mapper
* @createDate 2026-06-12 19:57:47
* @Entity com.zhuxiang.service.entity.LockPermission
*/
public interface LockPermissionMapper extends BaseMapper<LockPermission> {

    /** 原子插入 eKey 权限占位，唯一键冲突时保留原记录。 */
    @Insert("""
            INSERT INTO lock_permission
            (id, lease_id, tenant_id, house_id, smart_lock_id, ttlock_lock_id,
             receiver_username, permission_type, status, error_message,
             start_time, end_time, created_at, updated_at)
            VALUES
            (#{permission.id}, #{permission.leaseId}, #{permission.tenantId}, #{permission.houseId},
             #{permission.smartLockId}, #{permission.ttlockLockId}, #{permission.receiverUsername},
             #{permission.permissionType}, #{permission.status}, #{permission.errorMessage},
             #{permission.startTime}, #{permission.endTime}, #{permission.createdAt}, #{permission.updatedAt})
            ON DUPLICATE KEY UPDATE id = id
            """)
    int insertIfAbsent(@Param("permission") LockPermission permission);

    /** 锁定同一租约门锁的 eKey 记录，避免并发重复请求平台。 */
    @Select("""
            SELECT id, lease_id, tenant_id, house_id, smart_lock_id, ttlock_lock_id,
                   ttlock_key_id, receiver_username, permission_type, status,
                   error_message, start_time, end_time, created_at, updated_at
            FROM lock_permission
            WHERE lease_id = #{leaseId} AND tenant_id = #{tenantId}
              AND smart_lock_id = #{smartLockId} AND permission_type = 'EKEY'
            LIMIT 1 FOR UPDATE
            """)
    LockPermission selectForUpdate(
            @Param("leaseId") String leaseId,
            @Param("tenantId") String tenantId,
            @Param("smartLockId") String smartLockId
    );
}




