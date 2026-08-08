package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.dto.MessageUnreadCountRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author king-wang
* @description 针对表【message(用户消息表)】的数据库操作Mapper
* @createDate 2026-06-12 19:57:52
* @Entity com.zhuxiang.service.entity.Message
*/
public interface MessageMapper extends BaseMapper<Message> {

    List<MessageUnreadCountRow> selectUnreadCounts(@Param("userId") String userId);
}




