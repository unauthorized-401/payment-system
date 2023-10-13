package com.jiwon.payment.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(title = "결제 시스템 API 명세서",
                description = "결제 요청을 받아 카드사와 통신하는 인터페이스 제공",
                version = "v1"))
@Configuration
public class SwaggerConfig {
    @Bean
    public GroupedOpenApi chatOpenApi() {
        String[] paths = {
                "/common/payment/pay",              // 카드결제
                "/common/payment/cancel",           // 결제취소
                "/common/payment/cancel/partial",   // 부분취소
                "/common/payment/retrieve"          // 결제정보조회
        };

        return GroupedOpenApi.builder()
                .group("PAYMENT API v1")
                .pathsToMatch(paths)
                .build();
    }
}