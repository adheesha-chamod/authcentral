package com.adheesha.app.util;

import com.adheesha.app.audit.service.AuditService;
import com.adheesha.app.auth.dto.RegisterRequest;
import com.adheesha.app.exception.KeycloakException;
import com.adheesha.app.keycloak.client.KeycloakAdminClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataSeeder implements ApplicationRunner {

    private final KeycloakAdminClient keycloakAdminClient;
    private final AuditService auditService;

    public DataSeeder(KeycloakAdminClient keycloakAdminClient, AuditService auditService) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.auditService = auditService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUser("admin", "admin@app.com", "Admin@123", "App", "Admin", "Colombo", "ADMIN");
        seedUser("john", "john@app.com", "User@123", "John", "Doe", "Colombo", "USER");
    }

    private void seedUser(String username, String email, String password,
                          String firstName, String lastName, String address,
                          String usertype) {
        try {
            if (keycloakAdminClient.getUserByUsername(username).isPresent()) {
                log.info("Seed user '{}' already exists, skipping.", username);
                return;
            }
            RegisterRequest req = new RegisterRequest(username, email, password, firstName, lastName, address);
            String userId = keycloakAdminClient.createUser(req, usertype);
            keycloakAdminClient.assignRole(userId, usertype);
            auditService.log("SEED_USER_CREATED", username, usertype, "system");
            log.info("Seeded user '{}' with role '{}'.", username, usertype);
        } catch (KeycloakException e) {
            log.warn("Failed to seed user '{}': {} (status={})", username, e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            log.warn("Unexpected error seeding user '{}': {}", username, e.getMessage());
        }
    }
}
