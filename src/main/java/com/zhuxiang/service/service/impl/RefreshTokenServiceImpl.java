package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.RefreshToken;
import com.zhuxiang.service.service.RefreshTokenService;
import com.zhuxiang.service.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【refresh_token(用户刷新令牌表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:56
*/
@Service
public class RefreshTokenServiceImpl extends ServiceImpl<RefreshTokenMapper, RefreshToken>
    implements RefreshTokenService{

}




