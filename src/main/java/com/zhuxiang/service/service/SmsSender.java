package com.zhuxiang.service.service;

public interface SmsSender {

    void sendVerificationCode(String phone, String code);
}
