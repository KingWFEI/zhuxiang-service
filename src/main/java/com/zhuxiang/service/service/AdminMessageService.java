package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.AdminMessageDtos;

public interface AdminMessageService {

    /**
     * 由管理员向指定用户发送系统消息。
     *
     * @return 实际接收消息的用户数量
     */
    int sendSystemMessage(
            String operatorId,
            AdminMessageDtos.SendSystemMessageRequest request
    );
}
