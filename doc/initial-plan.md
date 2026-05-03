# Spring Boot + Keycloak Authentication Service — AI Implementation Specification

## Objective

Build a backend authentication and authorization service using:

* Java 17
* Spring Boot 4.x
* Spring Security
* Keycloak
* MySQL
* Docker Compose
* JWT Authentication

The project must follow clean enterprise backend development practices while remaining simple and maintainable.

---

# Core Requirements

The application must support the following features:

| Feature           | Access        |
| ----------------- | ------------- |
| Register          | Public        |
| Login             | Public        |
| Logout            | Authenticated |
| Refresh Token     | Public        |
| View Current User | Authenticated |
| List All Users    | ADMIN only    |
| View Audit Logs   | ADMIN only    |

---

# Important Architecture Rules

## Identity Management

Keycloak is the single source of truth for user identities.

DO NOT:

* create a local `users` table
* store passwords in the Spring Boot application
* generate JWT tokens manually
* access Keycloak database tables directly
* implement custom session handling

ALWAYS:

* use Keycloak Admin APIs
* use JWT validation via Spring Security Resource Server
* let Keycloak issue JWT access and refresh tokens
* use Keycloak for roles and permissions

---

# Technology Stack

| Component      | Technology               |
| -------------- | ------------------------ |
| Language       | Java 17                  |
| Framework      | Spring Boot 4.x          |
| Security       | Spring Security          |
| IAM            | Keycloak                 |
| Database       | MySQL 8                  |
| Build Tool     | Maven                    |
| Migration Tool | Flyway                   |
| Containers     | Docker + Docker Compose  |
| JWT            | OAuth2 JWT from Keycloak |

---

# Database Usage

## Application Database

The Spring Boot application database must contain ONLY:

* audit_logs table

There must NOT be:

* users table
* password table
* refresh token table
* session table

---

# Keycloak Usage

Keycloak stores:

* username
* password
* email
* firstName
* lastName
* address
* usertype
* email verification status
* roles

---

# User Types

A user can have ONLY ONE user type.

Allowed user types:

```text
ADMIN
USER
```

Keycloak realm roles must match these values exactly.

---

# Keycloak User Attributes

## Built-in Fields

Use Keycloak built-in fields for:

* username
* email
* firstName
* lastName

## Custom Attributes

Use Keycloak user attributes for:

* address
* usertype

Example:

```json
{
  "attributes": {
    "address": ["Colombo"],
    "usertype": ["USER"]
  }
}
```

---

# JWT Requirements

JWT tokens must be issued ONLY by Keycloak.

Spring Boot must ONLY validate JWTs.

DO NOT generate JWTs manually.

---

# Required JWT Claims

The JWT payload must include:

```json
{
  "sub": "uuid",
  "preferred_username": "john",
  "userId": "uuid",
  "username": "john",
  "firstName": "John",
  "lastName": "Doe",
  "address": "Colombo",
  "usertype": "USER",
  "realm_access": {
    "roles": ["USER"]
  },
  "iat": 111,
  "exp": 222
}
```

DO NOT include email in JWT claims.

---

# JWT Claim Mapping

Use Keycloak Protocol Mappers.

Map claims as follows:

| Claim     | Source             |
| --------- | ------------------ |
| userId    | user id            |
| username  | preferred_username |
| firstName | firstName          |
| lastName  | lastName           |
| address   | user attribute     |
| usertype  | user attribute     |

---

# Authentication Flow

## Login

Use Keycloak native OAuth2 token endpoint.

Endpoint:

```text
POST /realms/app-realm/protocol/openid-connect/token
```

Request type:

```text
application/x-www-form-urlencoded
```

Request:

```text
grant_type=password
client_id=spring-client
client_secret=secret
username=john
password=Password123
```

---

# Refresh Token Flow

Use Keycloak native token endpoint.

Request:

```text
grant_type=refresh_token
refresh_token=<token>
```

---

# Logout Flow

Use Keycloak native logout endpoint.

Endpoint:

```text
POST /realms/app-realm/protocol/openid-connect/logout
```

---

# Spring Boot Responsibilities

Spring Boot must provide only:

| API                        | Purpose                  |
| -------------------------- | ------------------------ |
| POST /api/v1/auth/register | Create users in Keycloak |
| GET /api/v1/users/me       | Current user details     |
| GET /api/v1/users          | List users               |
| GET /api/v1/audit-logs     | List audit logs          |

---

# Registration Requirements

## Registration Endpoint

```text
POST /api/v1/auth/register
```

## Request Body

```json
{
  "username": "john",
  "email": "john@test.com",
  "password": "Password123",
  "firstName": "John",
  "lastName": "Doe",
  "address": "Colombo"
}
```

## Registration Flow

1. Validate request
2. Create Keycloak user
3. Set user attributes
4. Assign USER role
5. Store address and usertype as attributes
6. Create audit log

Default usertype:

```text
USER
```

---

# Current User Endpoint

Endpoint:

```text
GET /api/v1/users/me
```

Requirements:

* authenticated users only
* extract user details from JWT claims
* DO NOT query database unnecessarily

Example response:

```json
{
  "userId": "uuid",
  "username": "john",
  "firstName": "John",
  "lastName": "Doe",
  "address": "Colombo",
  "usertype": "USER"
}
```

---

# List Users Endpoint

Endpoint:

```text
GET /api/v1/users
```

Requirements:

* ADMIN role only
* fetch users via Keycloak Admin API
* DO NOT query Keycloak DB tables directly

---

# Audit Logs

The application must store audit logs in MySQL.

## audit_logs Table

```sql
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(100) NOT NULL,
    username VARCHAR(100),
    user_type VARCHAR(20),
    ip_address VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

# Audit Events

Create audit logs for:

* registration
* login
* logout
* token refresh
* failed login
* list users access

---

# Security Rules

| Endpoint              | Access        |
| --------------------- | ------------- |
| /api/v1/auth/register | permitAll     |
| /api/v1/users/me      | authenticated |
| /api/v1/users         | ROLE_ADMIN    |
| /api/v1/audit-logs    | ROLE_ADMIN    |

---

# Spring Security Requirements

Use:

```text
Spring Security OAuth2 Resource Server
```

Application must be stateless.

Use:

```java
SessionCreationPolicy.STATELESS
```

---

# application.properties Requirements

Use ONLY:

```text
application.properties
```

DO NOT use:

* application.yml
* .env files

---

# Example application.properties

```properties
spring.application.name=auth-service

server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/app_db
spring.datasource.username=root
spring.datasource.password=root

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

spring.flyway.enabled=true

spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8181/realms/app-realm

keycloak.server-url=http://localhost:8181
keycloak.realm=app-realm
keycloak.client-id=spring-client
keycloak.client-secret=secret
keycloak.admin.username=admin
keycloak.admin.password=admin
```

---

# Docker Requirements

Use Docker Compose.

Required services:

```text
mysql
keycloak
app
```

---

# Keycloak Configuration

## Realm

```text
app-realm
```

## Client

```text
spring-client
```

Client type:

```text
confidential
```

Enable:

* Direct Access Grants
* Service Accounts

---

# Initial Data Requirements

On application startup:

1. Check whether ADMIN user exists
2. Create ADMIN user if missing
3. Check whether USER exists
4. Create sample USER if missing
5. Assign appropriate roles
6. Create audit logs

Use:

```text
ApplicationRunner
```

DO NOT seed Keycloak users via SQL scripts.

---

# Email Verification

DO NOT implement custom email verification APIs.

Use Keycloak native email verification support.

For development:

* manual verification via Keycloak dashboard is acceptable
* SMTP setup is NOT required

---

# Validation Requirements

Use Jakarta Validation.

Example annotations:

```java
@NotBlank
@Email
```

DO NOT implement custom password policy validation.

---

# Exception Handling

Use:

```java
@RestControllerAdvice
```

Provide standardized JSON error responses.

Example:

```json
{
  "timestamp": "2026-01-01T10:00:00",
  "status": 403,
  "message": "Access denied"
}
```

---

# Package Structure

Use the following package structure:

```text
com.example.authservice
├── auth
├── user
├── audit
├── keycloak
├── config
├── security
├── entity
├── repository
├── dto
├── exception
└── util
```

---

# Coding Rules

DO NOT:

* use field injection
* use Lombok @Data
* write business logic inside controllers
* use direct SQL for Keycloak
* manually parse JWTs

ALWAYS:

* use constructor injection
* separate controller/service/repository layers
* use DTOs
* use role-based authorization
* use Spring Security annotations

---

# Authorization Rules

Use:

```java
@PreAuthorize("hasRole('ADMIN')")
```

for admin-only APIs.

---

# Keycloak Access

The AI implementation must assume:

* Keycloak dashboard access is available
* protocol mappers can be configured manually
* roles can be configured manually
* realm/client setup can be configured manually

---

# Explicit Non-Requirements

DO NOT implement:

* Swagger/OpenAPI
* unit tests
* integration tests
* observability
* CORS
* HTTPS
* rate limiting
* Redis
* Kafka
* custom JWT generation
* custom authentication provider
* forgot password flow
* user database duplication
* custom session management

---

# Final Architecture Summary

```text
Client
  ↓
Spring Boot Resource Server
  ↓ validates JWT
Keycloak
  ↓
MySQL
```

Keycloak responsibilities:

* authentication
* authorization
* JWT issuing
* refresh tokens
* logout
* identity management
* roles
* user profile data

Spring Boot responsibilities:

* JWT validation
* protected APIs
* registration orchestration
* audit logging
* admin operations
