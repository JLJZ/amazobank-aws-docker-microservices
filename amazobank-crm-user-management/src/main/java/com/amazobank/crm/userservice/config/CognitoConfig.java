package com.amazobank.crm.userservice.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient() {
        Region region = parseRegionFromIssuerUri(issuerUri);
        return CognitoIdentityProviderClient.builder()
                .region(region)
                .build();
    }

    @Bean
    public String userPoolId() {
        return parseUserPoolIdFromIssuerUri(issuerUri);
    }

    private Region parseRegionFromIssuerUri(String issuerUri) {
        // Example: https://cognito-idp.ap-southeast-1.amazonaws.com/ap-southeast-1_W7C683l6w
        // Extract region from the URI
        Pattern pattern = Pattern.compile("cognito-idp\\.([^.]+)\\.amazonaws\\.com");
        Matcher matcher = pattern.matcher(issuerUri);
        if (matcher.find()) {
            return Region.of(matcher.group(1));
        }
        // Fallback or throw error if region cannot be parsed
        throw new IllegalArgumentException("Could not parse AWS region from Cognito issuer URI: " + issuerUri);
    }

    private String parseUserPoolIdFromIssuerUri(String issuerUri) {
        // Example: https://cognito-idp.ap-southeast-1.amazonaws.com/ap-southeast-1_W7C683l6w
        // Extract user pool ID from the URI
        int lastSlashIndex = issuerUri.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < issuerUri.length() - 1) {
            return issuerUri.substring(lastSlashIndex + 1);
        }
        throw new IllegalArgumentException("Could not parse User Pool ID from Cognito issuer URI: " + issuerUri);
    }
}
