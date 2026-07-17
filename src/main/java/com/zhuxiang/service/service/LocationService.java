package com.zhuxiang.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.AmapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final RestTemplate restTemplate;
    private final AmapProperties amapProperties;
    private final ObjectMapper objectMapper;

    public LocationService(RestTemplate restTemplate, AmapProperties amapProperties,
                           ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.amapProperties = amapProperties;
        this.objectMapper = objectMapper;
    }

    // ── POI 搜索结果 ──

    public record PoiItem(
            String mapProvider,
            String externalPoiId,
            String name,
            String province,
            String city,
            String district,
            String adCode,
            String address,
            BigDecimal longitude,
            BigDecimal latitude
    ) {}

    // ── 逆地理编码 (已有) ──

    public record ReverseGeoResult(String province, String city, String district) {}

    public record ReverseGeoFullResult(
            String province, String city, String district,
            String township, String neighborhood, String address
    ) {}

    // ── 高德 POI 文字搜索 ──

    public List<PoiItem> searchPoisByText(String keyword, String cityCode) {
        String url = UriComponentsBuilder.fromHttpUrl(amapProperties.getBaseUrl() + "/v3/place/text")
                .queryParam("key", amapProperties.getWebServiceKey())
                .queryParam("keywords", keyword)
                .queryParam("city", cityCode)
                .queryParam("citylimit", "true")
                .queryParam("types", "120300|120301")    // 住宅小区
                .queryParam("offset", 20)
                .queryParam("extensions", "base")
                .build(false)
                .toString();
        return fetchPois(url);
    }

    // ── 高德 POI 周边搜索 ──

    public List<PoiItem> searchPoisAround(String keyword, BigDecimal longitude, BigDecimal latitude, int radius) {
        String location = longitude + "," + latitude;
        String url = UriComponentsBuilder.fromHttpUrl(amapProperties.getBaseUrl() + "/v3/place/around")
                .queryParam("key", amapProperties.getWebServiceKey())
                .queryParam("location", location)
                .queryParam("keywords", keyword)
                .queryParam("radius", radius)
                .queryParam("types", "120300|120301")
                .queryParam("offset", 20)
                .queryParam("extensions", "base")
                .build(false)
                .toString();
        return fetchPois(url);
    }

    // ── 高德 POI 详情 ──

    public PoiItem getPoiDetail(String externalPoiId) {
        String url = UriComponentsBuilder.fromHttpUrl(amapProperties.getBaseUrl() + "/v3/place/detail")
                .queryParam("key", amapProperties.getWebServiceKey())
                .queryParam("id", externalPoiId)
                .queryParam("extensions", "base")
                .build(false)
                .toString();
        List<PoiItem> pois = fetchPois(url);
        return pois.isEmpty() ? null : pois.get(0);
    }

    // ── 逆地理编码 (已有) ──

    public ReverseGeoResult reverseGeocode(double lat, double lng) {
        JsonNode regeocode = fetchRegeocode(lat, lng);
        JsonNode comp = regeocode.path("addressComponent");
        String province = comp.path("province").asText("");
        String city = comp.path("city").asText("");
        if (city.isEmpty()) city = province;
        String district = comp.path("district").asText("");
        return new ReverseGeoResult(province, city, district);
    }

    public ReverseGeoFullResult reverseGeocodeFull(double lat, double lng) {
        JsonNode regeocode = fetchRegeocode(lat, lng);
        JsonNode comp = regeocode.path("addressComponent");
        String province = comp.path("province").asText("");
        String city = comp.path("city").asText("");
        if (city.isEmpty()) city = province;
        String district = comp.path("district").asText("");
        JsonNode townshipNode = comp.path("township");
        String township = townshipNode.isArray() && townshipNode.isEmpty() ? "" : townshipNode.asText("");
        JsonNode neighborhoodNode = comp.path("neighborhood");
        String neighborhood = neighborhoodNode.isObject() ? neighborhoodNode.path("name").asText("") : neighborhoodNode.asText("");
        String address = regeocode.path("formatted_address").asText("");
        return new ReverseGeoFullResult(province, city, district, township, neighborhood, address);
    }

    private JsonNode fetchRegeocode(double lat, double lng) {
        String url = amapProperties.getBaseUrl() + "/v3/geocode/regeo?location="
                + lng + "," + lat + "&key=" + amapProperties.getWebServiceKey();
        String body;
        try {
            body = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new IllegalStateException("调用高德逆地理编码接口失败", e);
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("解析高德逆地理编码响应失败", e);
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

    // ── 通用 POI 解析 ──

    private List<PoiItem> fetchPois(String url) {
        String body;
        try {
            body = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.warn("高德 POI 搜索请求失败", e);
            throw new BusinessException(50301, "地图服务暂时不可用，请稍后重试");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("高德 POI 搜索响应解析失败", e);
            throw new BusinessException(50301, "地图服务暂时不可用，请稍后重试");
        }
        String status = root.path("status").asText("");
        if (!"1".equals(status)) {
            String info = root.path("info").asText("");
            throw new BusinessException(50301, "地图搜索失败" + (info.isEmpty() ? "" : ": " + info));
        }
        JsonNode pois = root.path("pois");
        List<PoiItem> items = new ArrayList<>();
        if (pois.isArray()) {
            for (JsonNode poi : pois) {
                items.add(new PoiItem(
                        "amap",
                        poi.path("id").asText(""),
                        poi.path("name").asText(""),
                        poi.path("pname").asText(""),
                        poi.path("cityname").asText(""),
                        poi.path("adname").asText(""),
                        poi.path("adcode").asText(""),
                        poi.path("address").asText(""),
                        parseBigDecimal(poi.path("location").asText(""), true),
                        parseBigDecimal(poi.path("location").asText(""), false)
                ));
            }
        }
        return items;
    }

    private BigDecimal parseBigDecimal(String location, boolean isLongitude) {
        if (location == null || location.isEmpty()) return null;
        String[] parts = location.split(",");
        if (parts.length != 2) return null;
        try {
            return isLongitude ? new BigDecimal(parts[0]) : new BigDecimal(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
