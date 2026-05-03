package com.adheesha.app.keycloak.client;

import com.adheesha.app.auth.dto.RegisterRequest;
import com.adheesha.app.config.KeycloakProperties;
import com.adheesha.app.exception.KeycloakException;
import com.adheesha.app.keycloak.model.KeycloakRoleRepresentation;
import com.adheesha.app.keycloak.model.KeycloakUserRepresentation;
import com.adheesha.app.user.dto.UserResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KeycloakAdminClient {

    private final RestClient restClient;
    private final KeycloakProperties props;

    public KeycloakAdminClient(RestClient restClient, KeycloakProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    private String getAdminToken() {
        String url = props.getServerUrl() + "/realms/master/protocol/openid-connect/token";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", props.getAdmin().getUsername());
        form.add("password", props.getAdmin().getPassword());
        try {
            Map<String, Object> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
            return (String) response.get("access_token");
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Failed to obtain admin token", e.getStatusCode().value());
        }
    }

    public String createUser(RegisterRequest req, String usertype) {
        String token = getAdminToken();
        String url = props.getServerUrl() + "/admin/realms/" + props.getRealm() + "/users";

        Map<String, Object> body = Map.of(
            "username", req.username(),
            "email", req.email(),
            "firstName", req.firstName(),
            "lastName", req.lastName(),
            "enabled", true,
            "emailVerified", true,
            "attributes", Map.of(
                "address", List.of(req.address()),
                "usertype", List.of(usertype)
            ),
            "credentials", List.of(Map.of(
                "type", "password",
                "value", req.password(),
                "temporary", false
            ))
        );

        try {
            ResponseEntity<Void> response = restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

            String location = response.getHeaders().getFirst("Location");
            if (location == null) throw new KeycloakException("User created but location header missing", 500);
            return location.substring(location.lastIndexOf('/') + 1);
        } catch (HttpClientErrorException.Conflict e) {
            throw new KeycloakException("Username or email already exists", 409);
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Failed to create user: " + e.getMessage(), e.getStatusCode().value());
        }
    }

    public void assignRole(String userId, String roleName) {
        String token = getAdminToken();
        String roleUrl = props.getServerUrl() + "/admin/realms/" + props.getRealm() + "/roles/" + roleName;

        KeycloakRoleRepresentation role;
        try {
            role = restClient.get()
                .uri(roleUrl)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(KeycloakRoleRepresentation.class);
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Role '" + roleName + "' not found", e.getStatusCode().value());
        }

        String mappingUrl = props.getServerUrl() + "/admin/realms/" + props.getRealm()
            + "/users/" + userId + "/role-mappings/realm";
        try {
            restClient.post()
                .uri(mappingUrl)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Failed to assign role: " + e.getMessage(), e.getStatusCode().value());
        }
    }

    public List<UserResponse> getAllUsers() {
        String token = getAdminToken();
        String url = props.getServerUrl() + "/admin/realms/" + props.getRealm() + "/users?max=1000";
        try {
            List<KeycloakUserRepresentation> users = restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
            return users.stream().map(this::toUserResponse).toList();
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Failed to fetch users: " + e.getMessage(), e.getStatusCode().value());
        }
    }

    public Optional<KeycloakUserRepresentation> getUserByUsername(String username) {
        String token = getAdminToken();
        String url = props.getServerUrl() + "/admin/realms/" + props.getRealm()
            + "/users?username=" + username + "&exact=true";
        try {
            List<KeycloakUserRepresentation> users = restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
            return (users == null || users.isEmpty()) ? Optional.empty() : Optional.of(users.get(0));
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Failed to query users: " + e.getMessage(), e.getStatusCode().value());
        }
    }

    private UserResponse toUserResponse(KeycloakUserRepresentation u) {
        return new UserResponse(
            u.getId(),
            u.getUsername(),
            u.getFirstName(),
            u.getLastName(),
            u.getAttribute("address"),
            u.getAttribute("usertype")
        );
    }
}
