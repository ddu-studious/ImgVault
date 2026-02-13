package com.imgvault.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ImgVault API")
                        .description("统一图片存储与查询服务 - 轻量化混合架构 v2.1.0")
                        .version("2.1.0")
                        .contact(new Contact()
                                .name("ImgVault Team")));
    }
}
