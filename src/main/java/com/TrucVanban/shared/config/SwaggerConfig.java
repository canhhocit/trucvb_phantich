package com.TrucVanban.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String systemCodeScheme = "X-System-Code";
        final String apiKeyScheme = "X-API-Key";

        return new OpenAPI()
                .info(new Info()
                        .title("Trục Liên Thông Văn Bản API")
                        .description("Tài liệu API tích hợp Trục Liên Thông Văn Bản Quốc Gia")
                        .version("v1.0"))
                .addSecurityItem(new SecurityRequirement()
                        .addList(systemCodeScheme)
                        .addList(apiKeyScheme))
                .components(new Components()
                        .addSecuritySchemes(systemCodeScheme,
                                new SecurityScheme()
                                        .name("X-System-Code")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Nhập Mã hệ thống (ví dụ: SIMULATOR_CLIENT)"))
                        .addSecuritySchemes(apiKeyScheme,
                                new SecurityScheme()
                                        .name("X-API-Key")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Nhập Inbound API Key tương ứng trong db (vd : sim_inbound_ak_98765432101234567890)"))
                );
    }
}