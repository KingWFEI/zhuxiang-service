package com.zhuxiang.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.entity.LockPasscodePermission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 期限密码权限数据库访问。
 */
@Mapper
public interface LockPasscodePermissionMapper extends BaseMapper<LockPasscodePermission> {

    /** 原子插入占位记录，唯一键冲突时不覆盖现有权限。 */
    @Insert("""
            INSERT INTO lock_passcode_permission
            (id, lease_id, tenant_id, smart_lock_id, ttlock_lock_id,
             keyboard_pwd_type, keyboard_pwd_version, start_time, end_time,
             status, device_sync_status, error_message, created_at, updated_at)
            VALUES
            (#{permission.id}, #{permission.leaseId}, #{permission.tenantId}, #{permission.smartLockId},
             #{permission.ttlockLockId}, #{permission.keyboardPwdType}, #{permission.keyboardPwdVersion},
             #{permission.startTime}, #{permission.endTime}, #{permission.status},
             #{permission.deviceSyncStatus}, #{permission.errorMessage},
             #{permission.createdAt}, #{permission.updatedAt})
            ON DUPLICATE KEY UPDATE id = id
            """)
    int insertIfAbsent(@Param("permission") LockPasscodePermission permission);

    /** 锁定同一租约和门锁的权限记录，串行化生成或失败重试。 */
    @Select("""
            SELECT id, lease_id, tenant_id, smart_lock_id, ttlock_lock_id,
                   ttlock_keyboard_pwd_id, keyboard_pwd_ciphertext, keyboard_pwd_type,
                   keyboard_pwd_version, start_time, end_time, status,
                   device_sync_status, error_message, created_at, updated_at
            FROM lock_passcode_permission
            WHERE lease_id = #{leaseId} AND smart_lock_id = #{smartLockId}
            LIMIT 1 FOR UPDATE
            """)
    LockPasscodePermission selectForUpdate(
            @Param("leaseId") String leaseId,
            @Param("smartLockId") String smartLockId
    );
}
