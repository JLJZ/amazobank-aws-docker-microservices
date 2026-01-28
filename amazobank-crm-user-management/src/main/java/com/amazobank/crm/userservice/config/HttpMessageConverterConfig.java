package com.amazobank.crm.userservice.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * This is needed to support the ALB+Lambda integration. Lambda forward requests
 * as application/octet-stream. We want to convert it manually to application/json
 * so our rest controller can handle it.
 */
@Configuration
public class HttpMessageConverterConfig {

    @Bean
    public MappingJackson2HttpMessageConverter jacksonConverter() {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter();

        converter.setSupportedMediaTypes(
                List.of(
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_PROBLEM_JSON,
                    MediaType.APPLICATION_OCTET_STREAM  // Convert for octet-stream too
                )
        );

        return converter;
    }
}

