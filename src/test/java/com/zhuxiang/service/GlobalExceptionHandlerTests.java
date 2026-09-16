package com.zhuxiang.service;

import com.zhuxiang.service.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTests {

    @Test
    void missingResourceReturnsNotFoundInsteadOfInternalServerError() throws Exception {
        standaloneSetup(new MissingResourceController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(get("/missing-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("接口不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @RestController
    private static class MissingResourceController {

        @GetMapping("/missing-resource")
        void missingResource() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "missing-resource");
        }
    }
}
