package com.TrucVanban.shared.config;

import com.TrucVanban.shared.security.hmac.AgencyAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình Web MVC - đăng ký các interceptor.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AgencyAuthorizationInterceptor agencyAuthorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Đăng ký interceptor kiểm tra agency authorization
        // Interceptor này chỉ hoạt động khi endpoint có @RequireAgencyMatch
        registry.addInterceptor(agencyAuthorizationInterceptor)
                .addPathPatterns("/**"); // Apply to all paths, but only activate on @RequireAgencyMatch
    }
}
