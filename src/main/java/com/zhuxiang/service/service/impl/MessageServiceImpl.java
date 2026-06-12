package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.entity.Message;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.mapper.MessageMapper;
import org.springframework.stereotype.Service;

/**
* @author king-wang
* @description 针对表【message(用户消息表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:52
*/
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
    implements MessageService{

}




