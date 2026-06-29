package com.zhuxiang.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.zhuxiang.service.mapper")
@SpringBootApplication
@EnableScheduling
public class ZhuxiangServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZhuxiangServiceApplication.class, args);
	}

}
