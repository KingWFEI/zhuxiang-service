package com.zhuxiang.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class InternalHouseControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void existingAvailableHouseReturnsExistsTrueAndVisibleTrue() throws Exception {
        mockMvc.perform(get("/internal/houses/house-1/reference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.visible").value(true));
    }

    @Test
    void nonExistingHouseReturnsBothFalse() throws Exception {
        mockMvc.perform(get("/internal/houses/non-existent-id/reference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.visible").value(false));
    }

    @Test
    void internalEndpointAllowsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/internal/houses/house-1/reference"))
                .andExpect(status().isOk());
    }

    @Test
    void responseIsNotWrappedInApiResponse() throws Exception {
        mockMvc.perform(get("/internal/houses/house-1/reference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
