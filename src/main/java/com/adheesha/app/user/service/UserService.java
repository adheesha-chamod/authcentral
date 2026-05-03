package com.adheesha.app.user.service;

import com.adheesha.app.audit.service.AuditService;
import com.adheesha.app.keycloak.client.KeycloakAdminClient;
import com.adheesha.app.security.JwtClaimsExtractor;
import com.adheesha.app.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final JwtClaimsExtractor jwtClaimsExtractor;
    private final KeycloakAdminClient keycloakAdminClient;
    private final AuditService auditService;

    public UserService(JwtClaimsExtractor jwtClaimsExtractor,
                       KeycloakAdminClient keycloakAdminClient,
                       AuditService auditService) {
        this.jwtClaimsExtractor = jwtClaimsExtractor;
        this.keycloakAdminClient = keycloakAdminClient;
        this.auditService = auditService;
    }

    public UserResponse getCurrentUser(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwtClaimsExtractor.toUserResponse(jwt);
    }

    public List<UserResponse> getAllUsers(Authentication authentication, HttpServletRequest httpReq) {
        List<UserResponse> users = keycloakAdminClient.getAllUsers();
        auditService.log("LIST_USERS", authentication.getName(), "ADMIN", ipOf(httpReq));
        return users;
    }

    private String ipOf(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }
}
