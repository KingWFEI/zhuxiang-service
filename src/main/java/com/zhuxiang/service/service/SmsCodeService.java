package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.SmsCode;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.AuthDtos;

/**
* @author king-wang
* @description 针对表【sms_code(短信验证码表)】的数据库操作Service
* @createDate 2026-06-12 19:58:07
*/
public interface SmsCodeService extends IService<SmsCode> {

    /**
     * 生成并发送短信验证码。
     */
    AuthDtos.SmsCodeResult sendSmsCode(AuthDtos.SmsCodeRequest request);

    /**
     * 校验并核销短信验证码。
     */
    void consumeSmsCode(String phone, String scene, String code);
}
