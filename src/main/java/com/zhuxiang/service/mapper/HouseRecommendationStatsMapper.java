package com.zhuxiang.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhuxiang.service.entity.HouseRecommendationStats;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface HouseRecommendationStatsMapper extends BaseMapper<HouseRecommendationStats> {

    @Insert("""
            INSERT INTO house_recommendation_stats (
                house_id, exposure_count, detail_click_count, effective_view_count,
                favorite_count, appointment_count, order_count, not_interested_count
            ) VALUES (
                #{houseId},
                IF(#{eventType} = 'EXPOSURE', 1, 0),
                IF(#{eventType} = 'DETAIL_CLICK', 1, 0),
                IF(#{eventType} = 'EFFECTIVE_VIEW', 1, 0),
                IF(#{eventType} = 'FAVORITE', 1, 0),
                IF(#{eventType} = 'APPOINTMENT', 1, 0),
                IF(#{eventType} = 'ORDER_CREATED', 1, 0),
                IF(#{eventType} = 'NOT_INTERESTED', 1, 0)
            )
            ON DUPLICATE KEY UPDATE
                exposure_count = exposure_count + IF(#{eventType} = 'EXPOSURE', 1, 0),
                detail_click_count = detail_click_count + IF(#{eventType} = 'DETAIL_CLICK', 1, 0),
                effective_view_count = effective_view_count + IF(#{eventType} = 'EFFECTIVE_VIEW', 1, 0),
                favorite_count = favorite_count + IF(#{eventType} = 'FAVORITE', 1, 0),
                appointment_count = appointment_count + IF(#{eventType} = 'APPOINTMENT', 1, 0),
                order_count = order_count + IF(#{eventType} = 'ORDER_CREATED', 1, 0),
                not_interested_count = not_interested_count + IF(#{eventType} = 'NOT_INTERESTED', 1, 0),
                updated_at = CURRENT_TIMESTAMP
            """)
    int increment(@Param("houseId") String houseId, @Param("eventType") String eventType);
}
