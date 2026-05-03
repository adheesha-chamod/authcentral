package com.adheesha.app.audit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 100)
    private String username;

    @Column(length = 20)
    private String userType;

    @Column(length = 100)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
