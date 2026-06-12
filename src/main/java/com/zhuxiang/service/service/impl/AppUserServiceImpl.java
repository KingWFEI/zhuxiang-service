package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.AppUser;
import com.zhuxiang.service.service.AppUserService;
import com.zhuxiang.service.mapper.AppUserMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【app_user(移动端用户表)】的数据库操作Service实现
* @createDate 2026-06-12 19:55:54
*/
@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser>
    implements AppUserService{

}




