package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.HomeDtos;

/**
 * 首页聚合数据服务。
 */
public interface HomeService {

    /**
     * 获取首页首次加载所需的全部聚合数据。
     */
    HomeDtos.HomeData getHomeData(
            String cityCode,
            String region,
            Double latitude,
            Double longitude,
            long pageSize,
            String userId
    );
}
