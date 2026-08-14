package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.House;

import java.util.List;

public interface RecommendationRankingService {
    List<House> rank(List<House> candidates, String userId);
}
