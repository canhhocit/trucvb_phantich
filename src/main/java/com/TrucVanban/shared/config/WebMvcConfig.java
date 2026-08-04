package com.TrucVanban.shared.config;

import com.TrucVanban.shared.security.hmac.AgencyAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//đăng ký các interceptor
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AgencyAuthorizationInterceptor agencyAuthorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // chỉ hđ khi endpoint có @RequireAgencyMatch
        registry.addInterceptor(agencyAuthorizationInterceptor)
                .addPathPatterns("/**");
    }
}
