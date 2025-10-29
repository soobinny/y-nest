package com.example.capstonedesign.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정 클래스
 * <p>
 * 외부 API와의 HTTP 통신을 위해 RestTemplate Bean을 등록
 * 다른 서비스 클래스(예: FinlifeClient)에서 @Autowired 혹은 생성자 주입으로 사용 가능
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate Bean 등록
     * <p>
     * - Spring이 애플리케이션 구동 시 싱글톤으로 관리
     * - 외부 API 호출 시 HTTP 요청/응답을 처리하는 데 사용
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
