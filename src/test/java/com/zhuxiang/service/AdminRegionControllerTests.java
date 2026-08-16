package com.zhuxiang.service;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.controller.AdminRegionController;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.RegionService;
import com.zhuxiang.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminRegionControllerTests {

    private final RegionService regionService = mock(RegionService.class);
    private final UserService userService = mock(UserService.class);
    private final AdminRegionController controller = new AdminRegionController(regionService, userService);

    @Test
    void createsMultipleRegionsWithoutRequiringAdministrativeCode() {
        HttpServletRequest request = operatorRequest();

        controller.create(request, new AdminRegionController.SaveRequest(
                "成都市", "  ", "city", null, 0, true));
        controller.create(request, new AdminRegionController.SaveRequest(
                "绵阳市", null, "city", null, 1, true));

        ArgumentCaptor<Region> captor = ArgumentCaptor.forClass(Region.class);
        verify(regionService, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Region::getCode).containsOnlyNulls();
    }

    @Test
    void importsBlankAdministrativeCodeAsNull() {
        HttpServletRequest request = operatorRequest();
        when(regionService.list()).thenReturn(List.of());

        controller.importRegions(request, List.of(new AdminRegionController.ImportRequest(
                "成都市", "", "city", null, 0, true)));

        ArgumentCaptor<Region> captor = ArgumentCaptor.forClass(Region.class);
        verify(regionService).saveOrUpdate(captor.capture());
        assertThat(captor.getValue().getCode()).isNull();
    }

    private HttpServletRequest operatorRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(CurrentUser.USER_ID_ATTRIBUTE)).thenReturn("operator-1");
        User user = new User();
        user.setRole("ADMIN");
        when(userService.requireActiveUser("operator-1")).thenReturn(user);
        return request;
    }
}
