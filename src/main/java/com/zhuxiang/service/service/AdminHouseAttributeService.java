package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.AdminHouseAttributeDtos;

import java.util.List;

public interface AdminHouseAttributeService {

    List<AdminHouseAttributeDtos.FacilityItem> getFacilityDictionary(String operatorId);

    AdminHouseAttributeDtos.FacilityItem createFacility(
            AdminHouseAttributeDtos.CreateFacilityRequest request,
            String operatorId
    );

    AdminHouseAttributeDtos.FacilityItem updateFacility(
            String facilityId,
            AdminHouseAttributeDtos.UpdateFacilityRequest request,
            String operatorId
    );

    void deleteFacility(String facilityId, String operatorId);

    List<AdminHouseAttributeDtos.TagItem> getTagDictionary(String operatorId);

    AdminHouseAttributeDtos.TagItem createTag(
            AdminHouseAttributeDtos.CreateTagRequest request,
            String operatorId
    );

    AdminHouseAttributeDtos.TagItem updateTag(
            String tagId,
            AdminHouseAttributeDtos.UpdateTagRequest request,
            String operatorId
    );

    void deleteTag(String tagId, String operatorId);

    AdminHouseAttributeDtos.HouseAttributes getHouseAttributes(String houseId, String operatorId);

    AdminHouseAttributeDtos.HouseAttributes replaceFacilities(
            String houseId,
            List<String> facilityIds,
            String operatorId
    );

    AdminHouseAttributeDtos.HouseAttributes replaceTags(String houseId, List<String> tagIds, String operatorId);
}
