package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.Lease;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.ProfileDtos;

/**
* @author king-wang
* @description 针对表【lease(租约表)】的数据库操作Service
* @createDate 2026-06-12 19:57:39
*/
public interface LeaseService extends IService<Lease> {

    /**
     * 获取用户当前生效的租约。
     */
    ProfileDtos.CurrentHome getCurrentHome(String userId);
}
