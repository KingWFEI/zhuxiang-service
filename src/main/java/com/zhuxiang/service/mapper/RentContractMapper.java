package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.RentContract;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RentContractMapper extends BaseMapper<RentContract> {

    @Select("SELECT * FROM rent_contract WHERE order_id = #{orderId} LIMIT 1 FOR UPDATE")
    RentContract selectByOrderIdForUpdate(@Param("orderId") String orderId);
}
