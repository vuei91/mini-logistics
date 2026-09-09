package com.cjlogistics.mini.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 모든 @RestController 의 요청 경로에 "/api" 프리픽스를 자동으로 부여한다.
 * - CloudFront 에서 "/api/*" 만 백엔드(EC2)로 라우팅하고 나머지는 S3(프론트)로 보내기 위함.
 * - Swagger UI(/swagger-ui), API 문서(/v3/api-docs), H2 콘솔(/h2-console)은
 *   @RestController 가 아니므로 프리픽스가 붙지 않는다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 애플리케이션 자체 컨트롤러(com.cjlogistics.mini.*)에만 "/api" 프리픽스를 적용한다.
        // springdoc(OpenApiWebMvcResource 등) 도 @RestController 이므로 패키지로 필터링하지 않으면
        // /v3/api-docs 같은 문서 엔드포인트까지 "/api" 프리픽스가 붙어 접근이 깨진다.
        configurer.addPathPrefix("/api", clazz ->
                clazz.isAnnotationPresent(RestController.class)
                        && clazz.getPackageName().startsWith("com.cjlogistics.mini"));
    }
}
