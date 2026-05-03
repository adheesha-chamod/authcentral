# authcentral

Spring Boot 4.x OAuth2 Resource Server that delegates all identity management to Keycloak. See [doc/initial-plan.md](doc/initial-plan.md) for the full specification.

## Build & Run

```bash
# Build
./mvnw clean package -DskipTests

# Test
./mvnw test

# Run (requires MySQL + Keycloak via Docker Compose)
./mvnw spring-boot:run
```

## Architecture

```
Client → Spring Boot (JWT validation) → Keycloak (auth/identity)
                                      → MySQL (audit_logs only)
```

**Spring Boot owns:** JWT validation, registration orchestration, audit logging, protected API endpoints.  
**Keycloak owns:** authentication, JWT issuance, refresh tokens, logout, roles, user profiles.

## Critical Constraints

| Rule | Detail |
|------|--------|
| No local users table | Keycloak is the sole identity source |
| No manual JWT generation | Spring Boot only validates JWTs via OAuth2 Resource Server |
| Only `audit_logs` in MySQL | No users, sessions, or passwords in the app DB |
| Read user data from JWT | `GET /api/v1/users/me` reads claims — do NOT query any DB |
| List users via Admin API | `GET /api/v1/users` calls Keycloak Admin API — no DB query |
| No direct Keycloak DB access | Always use Keycloak REST APIs |

## Keycloak Configuration

- Realm: `app-realm` | Client: `spring-client` (confidential, Direct Access Grants enabled)
- Roles: `ADMIN`, `USER` (realm roles, one per user)
- Custom user attributes: `address`, `usertype`
- JWT claims mapped via Protocol Mappers: `userId`, `username`, `firstName`, `lastName`, `address`, `usertype`
- Email is NOT included in JWT claims

## Configuration

Use `application.properties` only — no `application.yml`, no `.env` files.

Required properties:
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8181/realms/app-realm
keycloak.server-url=http://localhost:8181
keycloak.realm=app-realm
keycloak.client-id=spring-client
keycloak.client-secret=secret
keycloak.admin.username=admin
keycloak.admin.password=admin
```

## Package Structure

```
com.adheesha.app
├── auth        # registration endpoint
├── user        # user endpoints
├── audit       # audit log entity, repo, service
├── keycloak    # Keycloak Admin API client
├── config      # Spring beans, security config
├── security    # JWT extraction helpers
├── entity      # JPA entities (AuditLog only)
├── repository  # Spring Data repos
├── dto         # request/response records
├── exception   # @RestControllerAdvice, error DTOs
└── util        # shared utilities
```

## Coding Rules

- **Constructor injection only** — no `@Autowired` field injection
- **No `@Data`** — use explicit `@Getter`/`@Builder`/constructors with Lombok
- **Controllers are thin** — all business logic in service layer
- **`@PreAuthorize("hasRole('ADMIN')")`** for admin endpoints
- **Jakarta Validation** (`@NotBlank`, `@Email`) on request DTOs
- **`@RestControllerAdvice`** for standardized error responses: `{ timestamp, status, message }`
- Session policy: `SessionCreationPolicy.STATELESS`

## API Surface

| Method | Path | Access |
|--------|------|--------|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Proxied to Keycloak token endpoint |
| POST | `/api/v1/auth/logout` | Authenticated |
| POST | `/api/v1/auth/refresh` | Public |
| GET | `/api/v1/users/me` | Authenticated |
| GET | `/api/v1/users` | `ROLE_ADMIN` |
| GET | `/api/v1/audit-logs` | `ROLE_ADMIN` |

## Database Migrations

Flyway migrations live in `src/main/resources/db/migration/`. Only schema: `audit_logs`.

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

## Not Implemented (by design)

Swagger, unit/integration tests, CORS, HTTPS, rate limiting, Redis, Kafka, forgot-password, custom session management, custom JWT generation.
