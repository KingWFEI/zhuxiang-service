package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.Conversation;
import com.zhuxiang.service.service.ConversationService;
import com.zhuxiang.service.mapper.ConversationMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【conversation(用户咨询会话表)】的数据库操作Service实现
* @createDate 2026-06-12 19:56:53
*/
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
    implements ConversationService{

}




