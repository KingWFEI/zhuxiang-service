package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.LockPermission;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author king-wang
* @description 针对表【lock_permission(门锁权限表)】的数据库操作Service
* @createDate 2026-06-12 19:57:47
*/
public interface LockPermissionService extends IService<LockPermission> {

    /**
     * 租约生效后为租客下发TTLock eKey；重复调用不会重复下发ACTIVE权限。
     */
    LockPermission grantTenantEKeyForLease(String leaseId);
}
