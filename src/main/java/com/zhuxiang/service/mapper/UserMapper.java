package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.dto.AdminSystemDtos;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author king-wang
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2026-06-12 19:55:54
* @Entity com.zhuxiang.service.entity.User
*/
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT
                role,
                COUNT(*) AS account_count,
                COALESCE(SUM(status = 'active'), 0) AS active_count,
                COALESCE(SUM(status = 'disabled'), 0) AS disabled_count,
                COALESCE(SUM(status = 'cancelled'), 0) AS cancelled_count,
                COALESCE(SUM(last_login_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY)), 0)
                    AS recent_login_count
            FROM `user`
            GROUP BY role
            """)
    @ConstructorArgs({
            @Arg(column = "role", javaType = String.class),
            @Arg(column = "account_count", javaType = long.class),
            @Arg(column = "active_count", javaType = long.class),
            @Arg(column = "disabled_count", javaType = long.class),
            @Arg(column = "cancelled_count", javaType = long.class),
            @Arg(column = "recent_login_count", javaType = long.class)
    })
    List<AdminSystemDtos.RoleStatisticsRow> selectRoleStatistics();
}
