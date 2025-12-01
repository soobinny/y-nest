package com.example.capstonedesign.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiConfig
 * -------------------------------------------------
 * - Swagger(OpenAPI 3) 기반의 API 문서 설정 클래스
 * - JWT 인증을 위한 BearerAuth 보안 스키마를 전역으로 등록
 * - 각 컨트롤러에 @SecurityRequirement(name = "bearerAuth") 적용 가능
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Capstone API", version = "v1"),
        security = { @SecurityRequirement(name = "bearerAuth") } // 전역 인증 요구 사항
)
@SecurityScheme(
        name = "bearerAuth",                     // Security 스키마 이름 (controller와 일치해야 함)
        type = SecuritySchemeType.HTTP,          // HTTP 인증 방식
        scheme = "bearer",                       // Bearer 토큰 기반
        bearerFormat = "JWT"                     // JWT 형식 지정
)
public class OpenApiConfig {

    /** Swagger Security 스키마 이름 상수 */
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * openAPI
     * -------------------------------------------------
     * - OpenAPI 문서 객체를 생성 및 설정
     * - Swagger UI 에서 보여질 제목, 버전, 설명, 인증 방식 등을 정의
     *
     * @return OpenAPI 인스턴스
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("청년 금융·주거 혜택 API") // Swagger 문서 제목
                        .version("v1")                 // 버전 정보
                        .description("회원/인증 등 사용자 API 문서")) // 설명
                // 전역 보안 요구 사항 등록
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                // BearerAuth 스키마를 components에 추가
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
