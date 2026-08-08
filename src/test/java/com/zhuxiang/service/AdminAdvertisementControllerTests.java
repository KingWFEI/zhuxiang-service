package com.zhuxiang.service;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.controller.AdminAdvertisementController;
import com.zhuxiang.service.dto.AdminAdvertisementDtos;
import com.zhuxiang.service.entity.Advertisement;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.AdvertisementService;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAdvertisementControllerTests {

    private final AdvertisementService advertisementService = mock(AdvertisementService.class);
    private final UserService userService = mock(UserService.class);
    private final HouseService houseService = mock(HouseService.class);
    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private final AdminAdvertisementController controller = new AdminAdvertisementController(
            advertisementService, userService, houseService, fileRecordService
    );

    @Test
    void createsScheduledBannerAndValidatesUploadedImageOwnership() {
        HttpServletRequest request = request("operator-1");
        when(userService.requireActiveUser("operator-1")).thenReturn(user("ADMIN"));
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusDays(2);
        AdminAdvertisementDtos.SaveRequest body = new AdminAdvertisementDtos.SaveRequest(
                "暑期租房季", "新客专享", "https://cdn.example.com/ad.jpg", "file-1",
                "none", null, "home_banner", true, 10, start, end
        );

        var response = controller.create(request, body);

        verify(fileRecordService).validateFileOwnership(
                "operator-1", "file-1", "https://cdn.example.com/ad.jpg", "advertisement_image"
        );
        ArgumentCaptor<Advertisement> captor = ArgumentCaptor.forClass(Advertisement.class);
        verify(advertisementService).save(captor.capture());
        assertThat(captor.getValue().getPosition()).isEqualTo("home_banner");
        assertThat(captor.getValue().getTargetType()).isEqualTo("none");
        assertThat(response.data().displayStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void rejectsLandlordRoleBeforeAdvertisementMutation() {
        HttpServletRequest request = request("landlord-1");
        when(userService.requireActiveUser("landlord-1")).thenReturn(user("LANDLORD"));

        assertThatThrownBy(() -> controller.delete(request, "ad-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void rejectsInvalidSchedule() {
        HttpServletRequest request = request("operator-1");
        when(userService.requireActiveUser("operator-1")).thenReturn(user("HOUSEKEEPER"));
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        AdminAdvertisementDtos.SaveRequest body = new AdminAdvertisementDtos.SaveRequest(
                "广告", null, "https://cdn.example.com/ad.jpg", "file-1",
                "none", null, "home_feed", true, 0, start, start.minusMinutes(1)
        );

        assertThatThrownBy(() -> controller.create(request, body))
                .isInstanceOf(BusinessException.class)
                .hasMessage("结束时间必须晚于开始时间");
    }

    private HttpServletRequest request(String userId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(CurrentUser.USER_ID_ATTRIBUTE)).thenReturn(userId);
        return request;
    }

    private User user(String role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}
