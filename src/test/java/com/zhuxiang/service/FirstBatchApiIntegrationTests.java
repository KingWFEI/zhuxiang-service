package com.zhuxiang.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.entity.AppUser;
import com.zhuxiang.service.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class FirstBatchApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserService appUserService;

    @Test
    void corsHeadersAreReturned() throws Exception {
        mockMvc.perform(options("/home/data")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Content-Type, Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string(
                        "Access-Control-Allow-Methods",
                        containsString("GET")
                ))
                .andExpect(header().string(
                        "Access-Control-Allow-Headers",
                        containsString("Authorization")
                ));

        mockMvc.perform(get("/home/data")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    void firstBatchMainFlowWorks() throws Exception {
        mockMvc.perform(get("/houses/feed")
                        .param("category", "recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].type").value("house"));

        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String phone = "13800138088";
        mockMvc.perform(post("/auth/sms-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", phone, "scene", "register"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresIn").value(300));

        String registerResponse = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "phone", phone,
                                "code", "123456",
                                "password", "123456",
                                "nickname", "King"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.phone").value(phone))
                .andReturn().getResponse().getContentAsString();

        JsonNode authData = objectMapper.readTree(registerResponse).path("data");
        String accessToken = authData.path("accessToken").asText();
        String refreshToken = authData.path("refreshToken").asText();
        String userId = authData.path("user").path("id").asText();
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(get("/profile").header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("King"));

        mockMvc.perform(put("/profile")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nickname", "King Wang"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("King Wang"));

        mockMvc.perform(post("/houses/house-1/favorite")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite").value(true));

        mockMvc.perform(get("/profile/favorite-houses")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("house-1"));

        mockMvc.perform(get("/home/data")
                        .header("Authorization", bearer(accessToken))
                        .param("cityCode", "500000")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.header.greeting", containsString("King Wang")))
                .andExpect(jsonPath("$.data.unreadMessageCount").value(1))
                .andExpect(jsonPath("$.data.houseGroups.recommended.items[0].house.isFavorite")
                        .value(true));

        mockMvc.perform(post("/appointments")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "houseId", "house-1",
                                "appointmentDate", LocalDate.now().plusDays(1).toString(),
                                "timeSlot", "10:00-11:00",
                                "contactName", "King",
                                "contactPhone", phone,
                                "remark", "上午看房"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"));

        mockMvc.perform(post("/rental-applications")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rentalRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        AppUser user = appUserService.getById(userId);
        user.setIsVerified(1);
        appUserService.updateById(user);

        mockMvc.perform(post("/rental-applications")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rentalRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"));

        mockMvc.perform(post("/conversations")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "source", "house_detail",
                                "houseId", "house-1",
                                "landlordId", "landlord-1"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").isNotEmpty());

        mockMvc.perform(get("/messages/unread-counts")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.system").value(1));

        String messagesResponse = mockMvc.perform(get("/messages")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("欢迎使用住享"))
                .andReturn().getResponse().getContentAsString();
        String messageId = objectMapper.readTree(messagesResponse)
                .path("data").path("items").path(0).path("id").asText();

        mockMvc.perform(put("/messages/{id}/read", messageId)
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(delete("/messages/read")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        mockMvc.perform(delete("/houses/house-1/favorite")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite").value(false));
    }

    @Test
    void searchAndDetailsMatchContract() throws Exception {
        mockMvc.perform(get("/home/data")
                        .param("cityCode", "500000")
                        .param("region", "渝北区")
                        .param("latitude", "29.6500000")
                        .param("longitude", "106.5500000")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.header.cityName").value("重庆"))
                .andExpect(jsonPath("$.data.unreadMessageCount").value(0))
                .andExpect(jsonPath("$.data.serviceEntries.length()").value(4))
                .andExpect(jsonPath("$.data.tabs.length()").value(4))
                .andExpect(jsonPath("$.data.houseGroups.recommended.items[0].type")
                        .value("house"))
                .andExpect(jsonPath("$.data.houseGroups.recommended.items[1].type")
                        .value("advertisement"))
                .andExpect(jsonPath("$.data.houseGroups.short_rent.items[0].house.id")
                        .value("house-4"))
                .andExpect(jsonPath("$.data.houseGroups.homestay.items").isEmpty())
                .andExpect(jsonPath("$.data.houseGroups.long_rent.items").isEmpty())
                .andExpect(jsonPath("$.data.advertisements").isArray());

        mockMvc.perform(get("/houses")
                        .param("keyword", "幸福小区")
                        .param("facilities", "air_conditioner,wifi")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].community").value("幸福小区"));

        mockMvc.perform(get("/houses/house-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.landlordId").value("landlord-1"))
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.facilities").isArray());

        mockMvc.perform(get("/houses/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.regions").isArray())
                .andExpect(jsonPath("$.data.sortOptions").isArray());
    }

    private String rentalRequest() throws Exception {
        return json(Map.of(
                "houseId", "house-1",
                "leaseStartDate", LocalDate.now().plusDays(10).toString(),
                "leaseMonths", 12,
                "remark", "希望尽快入住"
        ));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
