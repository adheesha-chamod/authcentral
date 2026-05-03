package com.adheesha.app.keycloak.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakUserRepresentation {

    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private Map<String, List<String>> attributes;

    public String getAttribute(String key) {
        if (attributes == null) return null;
        List<String> values = attributes.get(key);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
