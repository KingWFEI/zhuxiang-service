package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.HouseRoomTypeDtos;

import java.util.List;

public interface AdminHouseRoomTypeService {
    List<HouseRoomTypeDtos.Item> list(String operatorId);
    HouseRoomTypeDtos.Item create(HouseRoomTypeDtos.CreateRequest request, String operatorId);
    HouseRoomTypeDtos.Item update(String id, HouseRoomTypeDtos.UpdateRequest request, String operatorId);
    void delete(String id, String operatorId);
}
