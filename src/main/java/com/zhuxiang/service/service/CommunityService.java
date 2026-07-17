package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.CommunityDtos;
import com.zhuxiang.service.entity.Community;

public interface CommunityService extends IService<Community> {

    /** 搜索内部小区库 */
    CommunityDtos.SearchResponse searchCommunities(String keyword, String cityCode);

    /** 代理高德 POI 文字搜索 */
    CommunityDtos.PoiSearchResponse searchMapPois(String keyword, String cityCode);

    /** 代理高德 POI 周边搜索 */
    CommunityDtos.PoiSearchResponse searchMapPoisAround(String keyword, java.math.BigDecimal longitude,
                                                        java.math.BigDecimal latitude, int radius);

    /** 根据高德 POI ID 导入小区：已存在直接返回，否则调高德 detail API 去重后入库 */
    CommunityDtos.ImportResponse importFromMap(CommunityDtos.ImportRequest request);

    /** 合并重复小区 */
    void mergeCommunities(String sourceId, String targetId);
}
