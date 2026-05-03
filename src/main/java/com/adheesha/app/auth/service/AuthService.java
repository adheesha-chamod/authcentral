package com.adheesha.app.auth.service;

import com.adheesha.app.audit.service.AuditService;
import com.adheesha.app.auth.dto.*;
import com.adheesha.app.exception.KeycloakException;
import com.adheesha.app.keycloak.client.KeycloakAdminClient;
import com.adheesha.app.keycloak.client.KeycloakTokenClient;
import com.adheesha.app.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakTokenClient keycloakTokenClient;
    private final AuditService auditService;

    public AuthService(KeycloakAdminClient keycloakAdminClient,
                       KeycloakTokenClient keycloakTokenClient,
                       AuditService auditService) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakTokenClient = keycloakTokenClient;
        this.auditService = auditService;
    }

    public UserResponse register(RegisterRequest req, HttpServletRequest httpReq) {
        String userId = keycloakAdminClient.createUser(req, "USER");
        keycloakAdminClient.assignRole(userId, "USER");
        auditService.log("REGISTER", req.username(), "USER", ipOf(httpReq));
        return new UserResponse(userId, req.username(), req.firstName(), req.lastName(), req.address(), "USER");
    }

    public Map<String, Object> login(LoginRequest req, HttpServletRequest httpReq) {
        try {
            Map<String, Object> tokens = keycloakTokenClient.login(req.username(), req.password());
            auditService.log("LOGIN", req.username(), null, ipOf(httpReq));
            return tokens;
        } catch (KeycloakException e) {
            auditService.log("LOGIN_FAILED", req.username(), null, ipOf(httpReq));
            throw e;
        }
    }

    public void logout(LogoutRequest req, Authentication authentication, HttpServletRequest httpReq) {
        keycloakTokenClient.logout(req.refreshToken());
        auditService.log("LOGOUT", authentication.getName(), null, ipOf(httpReq));
    }

    public Map<String, Object> refresh(RefreshRequest req, HttpServletRequest httpReq) {
        Map<String, Object> tokens = keycloakTokenClient.refresh(req.refreshToken());
        auditService.log("TOKEN_REFRESH", null, null, ipOf(httpReq));
        return tokens;
    }

    private String ipOf(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }
}
