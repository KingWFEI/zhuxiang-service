package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.SmsCode;
import com.zhuxiang.service.service.SmsCodeService;
import com.zhuxiang.service.mapper.SmsCodeMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【sms_code(短信验证码表)】的数据库操作Service实现
* @createDate 2026-06-12 19:58:07
*/
@Service
public class SmsCodeServiceImpl extends ServiceImpl<SmsCodeMapper, SmsCode>
    implements SmsCodeService{

}




