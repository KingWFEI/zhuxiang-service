package com.zhuxiang.service;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminDashboardDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.AdminDashboardMapper;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class AdminDashboardServiceTests {

    private final AdminDashboardMapper mapper = mock(AdminDashboardMapper.class);
    private final UserService userService = mock(UserService.class);
    private final AdminDashboardServiceImpl service = new AdminDashboardServiceImpl(mapper, userService);

    @Test
    void returnsTwelveWeeksAndMarksUnsupportedHealthMetricsUnavailable() {
        User admin = user("ADMIN");
        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        when(mapper.selectAssetMetrics(null)).thenReturn(new AdminDashboardDtos.AssetMetrics(10, 6, 120, 300000));
        when(mapper.selectWorkflowMetrics(null)).thenReturn(new AdminDashboardDtos.WorkflowMetrics(2, 3, 600000, 8, 5, 900000, 4, 5, 1));
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        when(mapper.selectRentalTrend(any(), any(), org.mockito.ArgumentMatchers.isNull())).thenReturn(List.of(
                new AdminDashboardDtos.RentalTrendPoint(currentWeek, 3)
        ));
        when(mapper.selectRecentHouses(null)).thenReturn(List.of());

        var result = service.getOverview("admin-1");

        assertThat(result.rentalTrend()).hasSize(12);
        assertThat(result.rentalTrend().getLast().rentedCount()).isEqualTo(3);
        assertThat(result.health().houseDataCompletenessRate()).isNull();
        assertThat(result.house().totalCount()).isEqualTo(10);
    }

    @Test
    void rejectsNonManagementRole() {
        when(userService.requireActiveUser("tenant-1")).thenReturn(user("TENANT"));
        assertThatThrownBy(() -> service.getOverview("tenant-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(403));
    }

    @Test
    void scopesLandlordDashboardQueriesToCurrentLandlord() {
        User landlord = user("LANDLORD");
        landlord.setId("landlord-1");
        when(userService.requireActiveUser("landlord-1")).thenReturn(landlord);
        when(mapper.selectAssetMetrics("landlord-1")).thenReturn(new AdminDashboardDtos.AssetMetrics(0, 0, 0, 0));
        when(mapper.selectWorkflowMetrics("landlord-1")).thenReturn(new AdminDashboardDtos.WorkflowMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0));
        when(mapper.selectRentalTrend(any(), any(), org.mockito.ArgumentMatchers.eq("landlord-1"))).thenReturn(List.of());
        when(mapper.selectRecentHouses("landlord-1")).thenReturn(List.of());

        service.getOverview("landlord-1");

        verify(mapper).selectAssetMetrics("landlord-1");
        verify(mapper).selectWorkflowMetrics("landlord-1");
        verify(mapper).selectRecentHouses("landlord-1");
    }

    private User user(String role) {
        User user = new User();
        user.setRole(role);
        user.setStatus("active");
        return user;
    }
}
