package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDashboardDtos {

    private AdminDashboardDtos() {
    }

    @Schema(description = "房源资产汇总指标")
    public record AssetMetrics(
            @Schema(description = "房源总数") long totalCount,
            @Schema(description = "已绑定智能门锁房源数") long smartLockBoundCount,
            @Schema(description = "房源累计浏览量") long totalViewCount,
            @Schema(description = "平均月租，单位为分") long averageRent
    ) {
    }

    @Schema(description = "管理端业务待办汇总指标")
    public record WorkflowMetrics(
            @Schema(description = "合同已完成但尚未到入住日的待生效租约数") long pendingLeaseCount,
            @Schema(description = "本月待收账单数") long currentMonthOutstandingBillCount,
            @Schema(description = "本月待收金额，单位为分") long currentMonthOutstandingAmount,
            @Schema(description = "本月账单总数，不含已取消账单") long currentMonthBillCount,
            @Schema(description = "本月已支付账单数") long currentMonthPaidBillCount,
            @Schema(description = "本月已收金额，单位为分") long currentMonthReceivedAmount,
            @Schema(description = "待处理报修数") long pendingRepairCount,
            @Schema(description = "今日预约数") long todayAppointmentCount,
            @Schema(description = "今日已完成预约数") long todayCompletedAppointmentCount
    ) {
    }

    @Schema(description = "每周新增生效租约趋势点")
    public record RentalTrendPoint(
            @Schema(description = "周开始日期，星期一") LocalDate weekStart,
            @Schema(description = "该周开始生效的租约数") long rentedCount
    ) {
    }

    @Schema(description = "最近录入房源摘要")
    public record RecentHouse(
            @Schema(description = "房源 ID") String id,
            @Schema(description = "房源标题") String title,
            @Schema(description = "房源位置") String location,
            @Schema(description = "户型") String roomType,
            @Schema(description = "月租，单位为分") int price,
            @Schema(description = "是否已绑定智能门锁") boolean smartLockBound,
            @Schema(description = "录入时间") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "业务健康度指标，暂无可靠统计口径的字段返回空值")
    public record HealthMetrics(
            @Schema(description = "房源数据完整率百分比，暂无口径时为空") Integer houseDataCompletenessRate,
            @Schema(description = "租约续签及时率百分比，暂无口径时为空") Integer leaseRenewalTimelinessRate,
            @Schema(description = "报修按时完成率百分比，暂无口径时为空") Integer repairOnTimeCompletionRate
    ) {
        public static HealthMetrics unavailable() {
            return new HealthMetrics(null, null, null);
        }
    }

    @Schema(description = "管理端数据看板汇总")
    public record DashboardOverview(
            @Schema(description = "数据生成时间") LocalDateTime generatedAt,
            @Schema(description = "房源资产指标") AssetMetrics house,
            @Schema(description = "业务待办指标") WorkflowMetrics workflow,
            @Schema(description = "近十二周新增生效租约趋势") List<RentalTrendPoint> rentalTrend,
            @Schema(description = "业务健康度指标") HealthMetrics health,
            @Schema(description = "最近录入房源") List<RecentHouse> recentHouses
    ) {
    }
}
