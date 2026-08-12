package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.House;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
* @author king-wang
* @description 针对表【house(房源主表)】的数据库操作Mapper
* @createDate 2026-06-12 19:57:05
* @Entity com.zhuxiang.service.entity.House
*/
public interface HouseMapper extends BaseMapper<House> {

    @Select("SELECT * FROM house WHERE id = #{houseId} LIMIT 1 FOR UPDATE")
    House selectByIdForUpdate(@Param("houseId") String houseId);

    @Update("UPDATE house SET status = 'reserved', reserved_order_id = #{orderId}, " +
            "reserved_until = #{expiresAt}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{houseId} AND status = 'available' AND reserved_order_id IS NULL")
    int acquireReservation(@Param("houseId") String houseId,
                           @Param("orderId") String orderId,
                           @Param("expiresAt") java.time.LocalDateTime expiresAt);

    @Update("UPDATE house SET status = 'available', reserved_order_id = NULL, " +
            "reserved_until = NULL, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{houseId} AND reserved_order_id = #{orderId}")
    int releaseReservation(@Param("houseId") String houseId,
                           @Param("orderId") String orderId);

}




