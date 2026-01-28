package com.amazobank.crm.clientservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CognitoGroupGrantedAuthoritiesConverter());

        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/health").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/clients/**").hasRole("AGENT")
                    .anyRequest().denyAll()
                )
                .oauth2ResourceServer((oauth2) -> oauth2
                    .jwt(jwt -> jwt
                        .jwtAuthenticationConverter(converter)
                    )
                );
        return http.build();
    }
}
