package com.adheesha.app.auth.controller;

import com.adheesha.app.auth.dto.*;
import com.adheesha.app.auth.service.AuthService;
import com.adheesha.app.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req,
                                                  HttpServletRequest httpReq) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req, httpReq));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req,
                                                      HttpServletRequest httpReq) {
        return ResponseEntity.ok(authService.login(req, httpReq));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest req,
                                        Authentication authentication,
                                        HttpServletRequest httpReq) {
        authService.logout(req, authentication, httpReq);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshRequest req,
                                                        HttpServletRequest httpReq) {
        return ResponseEntity.ok(authService.refresh(req, httpReq));
    }
}
