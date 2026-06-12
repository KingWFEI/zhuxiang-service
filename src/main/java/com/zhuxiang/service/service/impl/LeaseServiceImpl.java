package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.mapper.LeaseMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【lease(租约表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:39
*/
@Service
public class LeaseServiceImpl extends ServiceImpl<LeaseMapper, Lease>
    implements LeaseService{

}




