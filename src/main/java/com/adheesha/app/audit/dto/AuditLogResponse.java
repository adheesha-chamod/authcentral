package com.adheesha.app.audit.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    String action,
    String username,
    String userType,
    String ipAddress,
    LocalDateTime createdAt
) {}
