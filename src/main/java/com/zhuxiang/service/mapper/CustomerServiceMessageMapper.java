package com.zhuxiang.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.entity.CustomerServiceMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CustomerServiceMessageMapper extends BaseMapper<CustomerServiceMessage> {

    /** 同一会话分配消息序号时锁定会话，避免并发产生重复序号。 */
    @Select("SELECT id FROM customer_service_session WHERE id = #{sessionId} FOR UPDATE")
    String lockSessionForMessage(@Param("sessionId") String sessionId);

    @Select("""
            SELECT COALESCE(MAX(sequence_no), 0) + 1
            FROM customer_service_message
            WHERE session_id = #{sessionId}
            """)
    Long selectNextSequenceNo(@Param("sessionId") String sessionId);
}
