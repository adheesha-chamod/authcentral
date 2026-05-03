package com.adheesha.app.keycloak.client;

import com.adheesha.app.config.KeycloakProperties;
import com.adheesha.app.exception.KeycloakException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class KeycloakTokenClient {

    private final RestClient restClient;
    private final KeycloakProperties props;

    public KeycloakTokenClient(RestClient restClient, KeycloakProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    public Map<String, Object> login(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("username", username);
        form.add("password", password);
        try {
            return postToTokenEndpoint(form);
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Invalid credentials", e.getStatusCode().value());
        }
    }

    public Map<String, Object> refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("refresh_token", refreshToken);
        try {
            return postToTokenEndpoint(form);
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Invalid or expired refresh token", e.getStatusCode().value());
        }
    }

    public void logout(String refreshToken) {
        String url = props.getServerUrl() + "/realms/" + props.getRealm()
            + "/protocol/openid-connect/logout";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("refresh_token", refreshToken);
        try {
            restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new KeycloakException("Logout failed", e.getStatusCode().value());
        }
    }

    private Map<String, Object> postToTokenEndpoint(MultiValueMap<String, String> form) {
        String url = props.getServerUrl() + "/realms/" + props.getRealm()
            + "/protocol/openid-connect/token";
        return restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }
}
