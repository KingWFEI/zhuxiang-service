package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminDashboardDtos;
import com.zhuxiang.service.dto.AdminDashboardDtos.DashboardOverview;
import com.zhuxiang.service.dto.AdminDashboardDtos.RentalTrendPoint;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.AdminDashboardMapper;
import com.zhuxiang.service.service.AdminDashboardService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final Set<String> MANAGEMENT_ROLES = Set.of("ADMIN", "HOUSEKEEPER", "LANDLORD");
    private final AdminDashboardMapper dashboardMapper;
    private final UserService userService;

    public AdminDashboardServiceImpl(AdminDashboardMapper dashboardMapper, UserService userService) {
        this.dashboardMapper = dashboardMapper;
        this.userService = userService;
    }

    @Override
    public DashboardOverview getOverview(String operatorId) {
        User operator = userService.requireActiveUser(operatorId);
        if (!MANAGEMENT_ROLES.contains(operator.getRole())) {
            throw BusinessException.forbidden("无权查看管理端数据看板");
        }
        String landlordId = "LANDLORD".equals(operator.getRole()) ? operator.getId() : null;

        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate start = currentWeek.minusWeeks(11);
        Map<LocalDate, RentalTrendPoint> existing = dashboardMapper
                .selectRentalTrend(start, currentWeek.plusWeeks(1), landlordId).stream()
                .collect(Collectors.toMap(RentalTrendPoint::weekStart, Function.identity()));
        var trend = new ArrayList<RentalTrendPoint>(12);
        for (int index = 0; index < 12; index++) {
            LocalDate weekStart = start.plusWeeks(index);
            trend.add(existing.getOrDefault(weekStart, new RentalTrendPoint(weekStart, 0)));
        }

        return new DashboardOverview(
                LocalDateTime.now(),
                dashboardMapper.selectAssetMetrics(landlordId),
                dashboardMapper.selectWorkflowMetrics(landlordId),
                trend,
                AdminDashboardDtos.HealthMetrics.unavailable(),
                dashboardMapper.selectRecentHouses(landlordId)
        );
    }
}
