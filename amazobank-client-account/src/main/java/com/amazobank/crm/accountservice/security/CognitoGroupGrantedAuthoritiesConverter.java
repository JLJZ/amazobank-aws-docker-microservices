package com.amazobank.crm.accountservice.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The JWT supplied by the user will look something like this:
 * {
    "sub": "a99a854c-7041-70d7-933f-5daba9f64404",
    "cognito:groups": [
        "Agent"
    ],
    "iss": "https://cognito-idp.ap-southeast-1.amazonaws.com/ap-southeast-1_W7C683l6w",
    "client_id": "5jqse721vu1hk4dmh1qvlml7b",
    "origin_jti": "a9f63c60-1544-4c6e-a4d7-6360a098c6a8",
    "event_id": "ca1db1a4-94e3-497b-9617-e0b5adefbf54",
    "token_use": "access",
    "scope": "aws.cognito.signin.user.admin",
    "auth_time": 1763111690,
    "exp": 1763115290,
    "iat": 1763111690,
    "jti": "cf78f512-cf26-4412-9a5d-d312e041d032",
    "username": "a99a854c-7041-70d7-933f-5daba9f64404"
    }

    This converter extracts each item in cognito:groups and create a matching SimpleGrantedAuthority for SpringWebSecurity FilterChain to use.
    E.g. "Agent" will map to "ROLE_AGENT"
 */
public class CognitoGroupGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @Nullable
    public Collection<GrantedAuthority> convert(@NonNull Jwt source) {
        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        List<String> groups = source.getClaimAsStringList("cognito:groups");

        for(String group : groups) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()));
        }
        return grantedAuthorities;
    }
    
}
