package com.adheesha.app.keycloak.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakRoleRepresentation {
    private String id;
    private String name;
}
