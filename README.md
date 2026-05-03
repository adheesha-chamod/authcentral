# authcentral

Spring Boot 4 OAuth2 Resource Server backed by Keycloak, with MySQL audit logging and Docker Compose infrastructure for local and containerized development.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Security | Spring Security + OAuth2 Resource Server (JWT) |
| Identity | Keycloak 26.5.6 |
| Database | MySQL 8.0 |
| Migrations | Flyway |
| HTTP Client | Spring RestClient |
| Containerization | Docker Compose |

## Architecture

```
Client → Spring Boot (JWT validation) → Keycloak (auth / identity)
                                      → MySQL  (audit_logs only)
```

- **Spring Boot** validates JWTs, orchestrates registration, and records audit events.
- **Keycloak** owns authentication, token issuance, refresh, logout, roles, and user profiles.
- **MySQL** stores only the `audit_logs` table — no local user table.

## Project Structure

```
com.adheesha.app
├── auth/           # Register, login, logout, refresh endpoints
├── user/           # Current user & list-all endpoints
├── audit/          # Audit log entity, repository, service, controller
├── keycloak/       # Keycloak Admin API + token clients, models
├── config/         # Security config, app config, Keycloak properties
├── security/       # JWT claims extractor
├── exception/      # Global exception handler, error DTO
└── util/           # Data seeder
```

## API Reference

| Method | Endpoint | Access |
|--------|----------|--------|
| `POST` | `/api/v1/auth/register` | Public |
| `POST` | `/api/v1/auth/login` | Public |
| `POST` | `/api/v1/auth/logout` | Authenticated |
| `POST` | `/api/v1/auth/refresh` | Public |
| `GET` | `/api/v1/users/me` | Authenticated |
| `GET` | `/api/v1/users` | `ROLE_ADMIN` |
| `GET` | `/api/v1/audit-logs` | `ROLE_ADMIN` |

## Prerequisites

- Java 17+
- Maven 3.9+ (or use the included `./mvnw`)
- Docker + Docker Compose

## Getting Started

### 1. Start infrastructure

```bash
docker compose --profile infra up -d
```

This starts:
- **MySQL** on host port `3307`
- **Keycloak** on host port `8181` with `app-realm` auto-imported

### 2. Run the application

```bash
./mvnw spring-boot:run
```

App starts on `http://localhost:8080`. On first boot, `DataSeeder` creates two users in Keycloak:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `Admin@123` | `ADMIN` |
| `john` | `User@123` | `USER` |

### 3. Full containerized stack

```bash
docker compose --profile infra --profile app up -d --build
```

## Configuration

All configuration is in [`src/main/resources/application.properties`](src/main/resources/application.properties).

Key properties for local development:

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/authcentral
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8181/realms/app-realm
keycloak.server-url=http://localhost:8181
keycloak.realm=app-realm
keycloak.client-id=spring-client
keycloak.client-secret=secret
```

## Keycloak Setup

The realm is auto-imported on Keycloak startup from [`infra/keycloak/app-realm-realm.json`](infra/keycloak/app-realm-realm.json).

| Setting | Value |
|---------|-------|
| Realm | `app-realm` |
| Client | `spring-client` (confidential) |
| Roles | `ADMIN`, `USER` |
| Custom JWT claims | `userId`, `username`, `firstName`, `lastName`, `address`, `usertype` |

## Postman Collection

Import [`authcentral.postman_collection.json`](authcentral.postman_collection.json) into Postman. The collection automatically captures `access_token` and `refresh_token` after login and injects them into subsequent requests.

## License

This project is licensed under the [MIT License](LICENSE).
