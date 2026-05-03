package com.adheesha.app.audit.service;

import com.adheesha.app.audit.dto.AuditLogResponse;
import com.adheesha.app.audit.entity.AuditLog;
import com.adheesha.app.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String username, String userType, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setUsername(username);
        log.setUserType(userType);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> getAllLogs() {
        return auditLogRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getAction(),
            log.getUsername(),
            log.getUserType(),
            log.getIpAddress(),
            log.getCreatedAt()
        );
    }
}
