package com.zhuxiang.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
		}
)
class ZhuxiangServiceApplicationTests {

	@Autowired
	private ServletWebServerApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void usesApiContextPath() {
		assertThat(applicationContext.getServletContext().getContextPath()).isEqualTo("/api");
	}

}
