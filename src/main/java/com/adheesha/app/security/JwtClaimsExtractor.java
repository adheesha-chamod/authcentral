package com.adheesha.app.security;

import com.adheesha.app.user.dto.UserResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimsExtractor {

    public UserResponse toUserResponse(Jwt jwt) {
        return new UserResponse(
            jwt.getClaimAsString("userId"),
            jwt.getClaimAsString("username"),
            jwt.getClaimAsString("firstName"),
            jwt.getClaimAsString("lastName"),
            jwt.getClaimAsString("address"),
            jwt.getClaimAsString("usertype")
        );
    }
}
