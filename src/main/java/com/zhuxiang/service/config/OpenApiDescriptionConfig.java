package com.zhuxiang.service.config;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static java.util.Map.entry;

/**
 * 为响应模型补充统一的中文字段说明，避免相同字段在多个 DTO 中重复维护。
 */
@Configuration
public class OpenApiDescriptionConfig {

    private static final Map<String, String> SCHEMA_DESCRIPTIONS = Map.ofEntries(
            entry("AdminHouseView", "管理端房源详情"),
            entry("Advertisement", "首页广告"),
            entry("AdvertisementView", "房源流广告"),
            entry("AppointmentResult", "预约看房提交结果"),
            entry("AuthResult", "登录或注册结果"),
            entry("AvatarResult", "头像上传结果"),
            entry("ContractPreviewResponse", "租赁合同预览"),
            entry("ConversationResult", "咨询会话创建结果"),
            entry("CurrentHome", "当前住所摘要"),
            entry("FavoriteResult", "房源收藏操作结果"),
            entry("FeedData", "房源流分页数据"),
            entry("FeedItem", "房源流条目，可为房源或广告"),
            entry("FileUploadResponse", "文件上传结果"),
            entry("FilterOptions", "房源搜索筛选选项"),
            entry("Header", "首页头部信息"),
            entry("HomeData", "首页聚合数据"),
            entry("HouseDetail", "房源完整详情"),
            entry("HouseView", "房源列表摘要"),
            entry("InitializeLockResponse", "门锁绑定或平台同步结果"),
            entry("LandlordView", "房东公开资料"),
            entry("LeaseItem", "租约详情"),
            entry("LeaseListResponse", "当前及历史租约列表"),
            entry("LocalInitializedLockResponse", "本地初始化门锁保存结果"),
            entry("LockDeviceView", "房源绑定的门锁摘要"),
            entry("LockInfo", "租约关联门锁和权限摘要"),
            entry("MessageView", "站内消息"),
            entry("Option", "通用筛选选项"),
            entry("PaymentInfoResponse", "订单支付信息"),
            entry("PriceRange", "租金筛选区间"),
            entry("RentOrderResponse", "租房订单详情"),
            entry("ServiceEntry", "首页服务入口"),
            entry("SmartLockByMacResponse", "按 MAC 查询的门锁摘要"),
            entry("SmartLockDetailResponse", "智能门锁管理详情"),
            entry("SmartLockUnlockDataResponse", "管理端蓝牙开锁数据"),
            entry("SmsCodeResult", "短信验证码发送结果"),
            entry("Tab", "首页房源标签页"),
            entry("TokenResult", "令牌刷新结果"),
            entry("UnreadCounts", "各分类未读消息数量"),
            entry("UserView", "用户公开及账户资料")
    );

    private static final Map<String, String> PROPERTY_DESCRIPTIONS = Map.ofEntries(
            entry("accessToken", "访问令牌"),
            entry("actionTarget", "点击操作目标"),
            entry("actionType", "点击操作类型"),
            entry("address", "详细地址"),
            entry("advertisement", "广告数据；条目为房源时为空"),
            entry("advertisements", "广告列表"),
            entry("appointment", "预约类未读消息数"),
            entry("area", "建筑面积，单位平方米"),
            entry("availableDate", "最早可入住日期"),
            entry("avatarUrl", "头像图片 URL"),
            entry("backgroundImageUrl", "背景图片 URL"),
            entry("battery", "门锁电量百分比"),
            entry("batteryLevel", "门锁电量百分比"),
            entry("batterySource", "电量数据来源"),
            entry("bill", "账单类未读消息数"),
            entry("billStatus", "账单状态"),
            entry("building", "楼栋"),
            entry("cancelledAt", "订单取消时间"),
            entry("category", "分类标识"),
            entry("cityName", "城市名称"),
            entry("community", "小区名称"),
            entry("communityId", "小区 ID"),
            entry("content", "消息正文"),
            entry("contractConfirmedAt", "合同确认时间"),
            entry("contractNo", "合同编号"),
            entry("contractStatus", "合同状态"),
            entry("conversationId", "咨询会话 ID"),
            entry("coverImage", "封面图片 URL"),
            entry("createdAt", "创建时间"),
            entry("currentLeases", "当前生效租约"),
            entry("decoration", "装修情况"),
            entry("deposit", "押金，单位元"),
            entry("description", "详细说明"),
            entry("enabled", "是否启用"),
            entry("endDate", "结束日期"),
            entry("expiresIn", "有效期，单位秒"),
            entry("facilities", "配套设施"),
            entry("favoriteCount", "收藏次数"),
            entry("fileId", "文件记录 ID"),
            entry("firstPaymentAmount", "首期应付金额，单位元"),
            entry("floor", "楼层描述"),
            entry("greeting", "首页问候语"),
            entry("hasMore", "是否还有下一页"),
            entry("hasPassword", "当前用户是否已设置登录密码"),
            entry("header", "首页头部信息"),
            entry("historyLeases", "历史租约"),
            entry("house", "房源数据；条目为广告时为空"),
            entry("houseAddress", "房源详细地址"),
            entry("houseGroups", "按标签页键分组的房源流"),
            entry("houseId", "房源 ID"),
            entry("houseImageUrl", "房源图片 URL"),
            entry("houseName", "房源名称"),
            entry("houseSummary", "房源摘要"),
            entry("iconKey", "前端图标键"),
            entry("id", "记录 ID"),
            entry("images", "房源图片列表"),
            entry("imageUrl", "图片 URL"),
            entry("isFavorite", "当前用户是否已收藏"),
            entry("isRead", "是否已读"),
            entry("isSelfViewingSupported", "是否支持自助看房"),
            entry("isSmartLockSupported", "是否支持智能门锁"),
            entry("isVerified", "是否已认证"),
            entry("items", "当前页条目"),
            entry("keeperName", "管家姓名"),
            entry("keeperPhone", "管家联系电话"),
            entry("key", "前端业务键"),
            entry("keyId", "开放平台电子钥匙 ID"),
            entry("label", "选项显示名称"),
            entry("landlordId", "房东用户 ID"),
            entry("landlordName", "房东姓名"),
            entry("lastBleSyncTime", "最近一次蓝牙同步时间"),
            entry("lastPlatformSyncTime", "最近一次开放平台同步时间"),
            entry("lease", "租约类未读消息数"),
            entry("leaseId", "租约 ID"),
            entry("leaseMonths", "租期月数"),
            entry("leaseStatus", "租约状态"),
            entry("location", "区域或商圈位置"),
            entry("lock", "门锁类未读消息数"),
            entry("lockBrand", "门锁品牌"),
            entry("lockData", "蓝牙 SDK 门锁初始化数据，敏感字段"),
            entry("lockDevice", "绑定的智能门锁；未绑定时为空"),
            entry("lockId", "门锁 ID"),
            entry("lockMac", "门锁 MAC 地址"),
            entry("lockName", "门锁名称"),
            entry("lockPermissionStatus", "门锁权限状态"),
            entry("lockSn", "门锁序列号"),
            entry("lockStatus", "门锁状态"),
            entry("maxPrice", "最高月租金，单位元"),
            entry("metro", "附近地铁信息"),
            entry("minPrice", "最低月租金，单位元"),
            entry("monthlyRent", "月租金，单位元"),
            entry("name", "名称"),
            entry("nickname", "用户昵称"),
            entry("orderId", "租房订单 ID"),
            entry("orientation", "房屋朝向"),
            entry("page", "当前页码，从 1 开始"),
            entry("pageSize", "每页条数"),
            entry("paidAt", "付款完成时间"),
            entry("paymentDay", "每月付款日"),
            entry("paymentMethod", "租金付款方式"),
            entry("paymentMonths", "每期支付月数"),
            entry("permissionStatus", "开锁权限状态"),
            entry("phone", "手机号"),
            entry("platformErrorCode", "开放平台错误码"),
            entry("platformErrorMessage", "开放平台错误信息"),
            entry("position", "广告展示位置"),
            entry("price", "月租金，单位元"),
            entry("priceRanges", "租金区间选项"),
            entry("rating", "综合评分"),
            entry("realNameAt", "实名认证提交时间"),
            entry("refreshToken", "刷新令牌"),
            entry("regions", "区域选项"),
            entry("rentedCount", "累计出租次数"),
            entry("rentType", "租赁类型"),
            entry("repair", "报修类未读消息数"),
            entry("requiresLogin", "入口是否要求登录"),
            entry("responseDescription", "响应速度说明"),
            entry("role", "用户角色"),
            entry("room", "房号"),
            entry("roomId", "房间 ID"),
            entry("roomName", "房间名称"),
            entry("roomType", "户型"),
            entry("roomTypes", "户型选项"),
            entry("rssi", "蓝牙信号强度，单位 dBm"),
            entry("searchPlaceholder", "搜索框提示文字"),
            entry("serviceEntries", "服务入口列表"),
            entry("serviceFee", "服务费，单位元"),
            entry("signedAt", "签约完成时间"),
            entry("smartLockBound", "是否已绑定智能门锁"),
            entry("smartLockId", "智能门锁本地记录 ID"),
            entry("sort", "排序序号"),
            entry("sortOptions", "排序选项"),
            entry("startDate", "开始日期"),
            entry("status", "业务状态"),
            entry("subtitle", "副标题"),
            entry("system", "系统类未读消息数"),
            entry("tabs", "首页房源标签页"),
            entry("tags", "房源标签"),
            entry("targetType", "跳转目标类型"),
            entry("targetValue", "跳转目标值"),
            entry("tenantCount", "租住人数"),
            entry("tenantIdCard", "租客身份证号"),
            entry("tenantName", "租客姓名"),
            entry("tenantPhone", "租客联系电话"),
            entry("title", "标题"),
            entry("total", "总记录数或合计数量"),
            entry("totalAmount", "订单总金额，单位元"),
            entry("type", "条目类型"),
            entry("unit", "单元"),
            entry("unreadMessageCount", "当前用户未读消息总数"),
            entry("updatedAt", "最后更新时间"),
            entry("url", "文件访问 URL"),
            entry("user", "用户资料"),
            entry("userId", "用户 ID"),
            entry("validFrom", "权限生效时间"),
            entry("validTo", "权限失效时间"),
            entry("value", "选项提交值"),
            entry("viewCount", "浏览次数")
    );

    @Bean
    public OpenApiCustomizer chineseSchemaDescriptions() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().forEach((schemaName, schema) -> {
                if (schema.getDescription() == null) {
                    schema.setDescription(SCHEMA_DESCRIPTIONS.get(schemaName));
                }
                if (schema.getProperties() == null) {
                    return;
                }
                schema.getProperties().forEach((propertyName, property) -> {
                    Schema<?> propertySchema = (Schema<?>) property;
                    if (propertySchema.getDescription() == null) {
                        propertySchema.setDescription(PROPERTY_DESCRIPTIONS.get(propertyName));
                    }
                });
            });
        };
    }

    /**
     * 统一补充接口成功和异常响应，保证导出的文档能说明通用错误语义。
     */
    @Bean
    public OpenApiCustomizer commonResponseDescriptions() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().stream()
                    .flatMap(pathItem -> pathItem.readOperations().stream())
                    .forEach(operation -> {
                        if (operation.getResponses().get("200") != null) {
                            operation.getResponses().get("200").setDescription("请求成功");
                        }
                        operation.getResponses().putIfAbsent("400", errorResponse("请求参数错误或业务校验失败"));
                        if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
                            operation.getResponses().putIfAbsent("401", errorResponse("未登录、访问令牌缺失或已失效"));
                            operation.getResponses().putIfAbsent("403", errorResponse("当前用户无权执行该操作"));
                        }
                        operation.getResponses().putIfAbsent("404", errorResponse("请求的业务资源不存在"));
                        operation.getResponses().putIfAbsent("409", errorResponse("资源状态冲突，无法完成当前操作"));
                        operation.getResponses().putIfAbsent("500", errorResponse("服务器内部错误"));
                    });
        };
    }

    private static io.swagger.v3.oas.models.responses.ApiResponse errorResponse(String description) {
        Schema<?> errorSchema = new Schema<>().$ref("#/components/schemas/ApiResponseVoid");
        return new io.swagger.v3.oas.models.responses.ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        "application/json",
                        new MediaType().schema(errorSchema)
                ));
    }
}
