package com.zhuxiang.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.entity.HouseRentalReservation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface HouseRentalReservationMapper extends BaseMapper<HouseRentalReservation> {
    @Select("SELECT * FROM house_rental_reservation WHERE order_id = #{orderId} LIMIT 1 FOR UPDATE")
    HouseRentalReservation selectByOrderIdForUpdate(@Param("orderId") String orderId);

    @Update("UPDATE house_rental_reservation SET status = #{targetStatus}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE order_id = #{orderId} AND status = 'ACTIVE'")
    int transitionActive(@Param("orderId") String orderId, @Param("targetStatus") String targetStatus);
}
