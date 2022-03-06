package com.kdy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			// 전체 설정
			.allowedOrigins("*")
			.allowedMethods("GET", "POST");
			// 특정 url 설정
			//.allowedOrigins("http://localhost:8080", "http://localhost:8081");
	}
}
