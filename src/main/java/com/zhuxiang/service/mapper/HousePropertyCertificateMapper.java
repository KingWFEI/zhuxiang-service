package com.zhuxiang.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.entity.HousePropertyCertificate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface HousePropertyCertificateMapper extends BaseMapper<HousePropertyCertificate> {

    @Select("""
            SELECT *
            FROM house_property_certificate
            WHERE house_id = #{houseId} AND is_current = 1
            ORDER BY created_at DESC
            LIMIT 1
            """)
    HousePropertyCertificate selectCurrent(@Param("houseId") String houseId);

    @Select("""
            SELECT *
            FROM house_property_certificate
            WHERE house_id = #{houseId}
            ORDER BY created_at DESC
            """)
    List<HousePropertyCertificate> selectHistoryByHouseId(
            @Param("houseId") String houseId);

    @Update("""
            UPDATE house_property_certificate
            SET is_current = 0
            WHERE house_id = #{houseId} AND is_current = 1
            """)
    int clearCurrent(@Param("houseId") String houseId);
}
