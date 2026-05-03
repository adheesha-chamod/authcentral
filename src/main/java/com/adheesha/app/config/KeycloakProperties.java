package com.adheesha.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private Admin admin = new Admin();

    @Getter
    @Setter
    public static class Admin {
        private String username;
        private String password;
    }
}
