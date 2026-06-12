package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.ConversationMessage;
import com.zhuxiang.service.service.ConversationMessageService;
import com.zhuxiang.service.mapper.ConversationMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【conversation_message(用户咨询会话消息表)】的数据库操作Service实现
* @createDate 2026-06-12 19:56:58
*/
@Service
public class ConversationMessageServiceImpl extends ServiceImpl<ConversationMessageMapper, ConversationMessage>
    implements ConversationMessageService{

}




