package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
    웹 애플리케이션 전역 설정
        - HTTP 메서드 오버라이드 지원 (PUT, DELETE 등)

    HiddenHttpMethodFilter Bean 등록
        - 역할
            - HTML form은 GET, POST만 지원함
            - PUT, PATCH, DELETE를 사용할 수 없음
            - _method 파라미터로 실제 HTTP 메서드를 전달하면 이를 인식하여 변환
        - <form method="POST" action="boards/119">
            <input type="hidden" name="_method" value="DELETE">
            <!-- Spring이 이를 DELETE 요청으로 인식 -->
          </form>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter(){
        return new HiddenHttpMethodFilter();
    }

    /*
    이미지파일은 게시글에 이미지 표시하는 기능
    정적 리소스 핸들러 설정
        - /uploads/** 요청 → uploads/ 폴더에서 파일 제공
        - 이미지, 파일 등을 브라우저에서 직접 접근 가능하도록 설정
 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
}
}
