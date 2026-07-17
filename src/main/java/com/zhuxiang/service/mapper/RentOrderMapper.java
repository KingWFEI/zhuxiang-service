package com.zhuxiang.service.mapper;

import com.zhuxiang.service.entity.RentOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RentOrderMapper extends BaseMapper<RentOrder> {

    @Select("""
            SELECT ro.*
            FROM rent_order ro
            INNER JOIN rent_contract rc ON rc.order_id = ro.id
            WHERE ro.lessor_user_id = #{lessorUserId}
              AND ro.status = 'pendingEsign'
              AND rc.lessor_signed = 0
              AND rc.status NOT IN ('signed', 'canceled', 'expired')
            ORDER BY COALESCE(rc.updated_at, ro.updated_at) DESC
            """)
    IPage<RentOrder> selectLandlordPendingSignPage(
            Page<RentOrder> page,
            @Param("lessorUserId") String lessorUserId);
}
