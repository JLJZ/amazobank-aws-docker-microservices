package com.amazobank.crm.clientservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class SqsConfig {
    
    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
            .credentialsProvider(
                DefaultCredentialsProvider.builder()
                    .asyncCredentialUpdateEnabled(true)
                    .reuseLastProviderEnabled(true)
                    .build()
            ).build();
    }
}
