package com.zhuxiang.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.AmapProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 位置服务（逆地理编码等）。
 */
@Service
public class LocationService {

    private final RestTemplate restTemplate;
    private final AmapProperties amapProperties;
    private final ObjectMapper objectMapper;

    public LocationService(RestTemplate restTemplate, AmapProperties amapProperties,
                           ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.amapProperties = amapProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 逆地理编码结果（基础版，到区级）。
     */
    public record ReverseGeoResult(String province, String city, String district) {
    }

    /**
     * 逆地理编码结果（完整版）。
     */
    public record ReverseGeoFullResult(
            String province, String city, String district,
            String township, String neighborhood, String address
    ) {
    }

    /**
     * 根据经纬度调用高德逆地理编码接口，返回省/市/区。
     */
    public ReverseGeoResult reverseGeocode(double lat, double lng) {
        JsonNode regeocode = fetchRegeocode(lat, lng);
        JsonNode comp = regeocode.path("addressComponent");
        String province = comp.path("province").asText("");
        String city = comp.path("city").asText("");
        if (city.isEmpty()) {
            city = province;
        }
        String district = comp.path("district").asText("");
        return new ReverseGeoResult(province, city, district);
    }

    /**
     * 根据经纬度调用高德逆地理编码接口，返回省/市/区/街道/社区/格式化地址。
     */
    public ReverseGeoFullResult reverseGeocodeFull(double lat, double lng) {
        JsonNode regeocode = fetchRegeocode(lat, lng);
        JsonNode comp = regeocode.path("addressComponent");
        String province = comp.path("province").asText("");
        String city = comp.path("city").asText("");
        if (city.isEmpty()) {
            city = province;
        }
        String district = comp.path("district").asText("");
        JsonNode townshipNode = comp.path("township");
        String township = townshipNode.isArray() && townshipNode.isEmpty()
                ? "" : townshipNode.asText("");
        JsonNode neighborhoodNode = comp.path("neighborhood");
        String neighborhood = neighborhoodNode.isObject()
                ? neighborhoodNode.path("name").asText("")
                : neighborhoodNode.asText("");
        String address = regeocode.path("formatted_address").asText("");
        return new ReverseGeoFullResult(province, city, district, township, neighborhood, address);
    }

    /**
     * 调用高德逆地理编码 API 并返回 regeocode 节点。
     */
    private JsonNode fetchRegeocode(double lat, double lng) {
        String url = "https://restapi.amap.com/v3/geocode/regeo?location="
                + lng + "," + lat + "&key=" + amapProperties.getKey();
        String body;
        try {
            body = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new IllegalStateException("调用高德逆地理编码接口失败: " + e.getMessage(), e);
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("解析高德逆地理编码响应失败: " + e.getMessage(), e);
        }
        if (!"1".equals(root.path("status").asText(""))) {
            throw BusinessException.badRequest("逆地理编码查询失败，请检查坐标参数");
        }
        JsonNode regeocode = root.path("regeocode");
        if (regeocode.isMissingNode() || regeocode.path("addressComponent").isMissingNode()) {
            throw BusinessException.badRequest("逆地理编码返回数据不完整");
        }
        return regeocode;
    }
}
