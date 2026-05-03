# authcentral

Spring Boot 4.x OAuth2 Resource Server delegating all identity to Keycloak. Full spec: [doc/initial-plan.md](doc/initial-plan.md).

## Build & Run

```bash
# Start infrastructure (MySQL on :3307, Keycloak on :8181)
docker compose --profile infra up -d

# Run app locally (needs infra running)
./mvnw spring-boot:run

# Full containerised stack
docker compose --profile infra --profile app up -d --build

# Build JAR only
./mvnw clean package -DskipTests
```

> **Local vs container:** `application.properties` uses `localhost:3307` / `localhost:8181`.  
> The `app` Docker service overrides those via environment variables (`mysql:3306`, `keycloak:8080`).

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
| Read user data from JWT | `GET /api/v1/users/me` reads JWT claims — do NOT query any DB |
| List users via Admin API | `GET /api/v1/users` calls Keycloak Admin API — no DB query |
| No direct Keycloak DB access | Always use Keycloak REST APIs |

## Package Structure

Feature-vertical layout — each feature owns its own controller/service/dto/entity/repository:

```
com.adheesha.app
├── auth/
│   ├── controller/AuthController
│   ├── service/AuthService
│   └── dto/  (RegisterRequest, LoginRequest, LogoutRequest, RefreshRequest)
├── user/
│   ├── controller/UserController
│   ├── service/UserService
│   └── dto/UserResponse
├── audit/
│   ├── controller/AuditLogController
│   ├── service/AuditService
│   ├── entity/AuditLog
│   ├── repository/AuditLogRepository
│   └── dto/AuditLogResponse
├── keycloak/
│   ├── client/  (KeycloakAdminClient, KeycloakTokenClient)
│   └── model/   (KeycloakUserRepresentation, KeycloakRoleRepresentation)
├── config/      (AppConfig, SecurityConfig, KeycloakProperties)
├── security/    (JwtClaimsExtractor)
├── exception/   (GlobalExceptionHandler, KeycloakException, ErrorResponse)
└── util/        (DataSeeder)
```

## Coding Rules

- **Constructor injection only** — no `@Autowired` field injection
- **No `@Data`** — use explicit `@Getter`/`@Builder`/constructors with Lombok
- **Controllers are thin** — all business logic in service layer
- **`@PreAuthorize("hasRole('ADMIN')")`** for admin endpoints
- **Jakarta Validation** (`@NotBlank`, `@Email`) on request DTOs
- **`@RestControllerAdvice`** for standardised error responses: `{ timestamp, status, message }` — see `GlobalExceptionHandler`
- **`application.properties` only** — no `application.yml`, no `.env` files
- Session policy: `SessionCreationPolicy.STATELESS`

## JWT & Security Details

- JWT roles extracted from `realm_access.roles` claim, prefixed with `ROLE_` → `SimpleGrantedAuthority`
- JWT claims in access token: `userId`, `username`, `firstName`, `lastName`, `address`, `usertype` (email is **not** a claim)
- `JwtClaimsExtractor.toUserResponse(Jwt)` maps all claims to `UserResponse`
- `usertype` on registration is hardcoded to `"USER"` — only `DataSeeder` and `KeycloakAdminClient.assignRole` control role assignment

## Keycloak Configuration

- Realm: `app-realm` (auto-imported from [infra/keycloak/app-realm-realm.json](infra/keycloak/app-realm-realm.json) via `--import-realm`)
- Client: `spring-client` | secret: `secret` | Direct Access Grants + Service Accounts enabled
- Roles: `ADMIN`, `USER` (realm roles)
- Custom user attributes: `address`, `usertype` (mapped via `oidc-usermodel-attribute-mapper`)
- Admin API calls use a short-lived token fetched from the `master` realm `admin-cli` client per request

## Seed Users (created by DataSeeder on startup)

| Username | Password | Role |
|----------|----------|------|
| `admin` | `Admin@123` | `ADMIN` |
| `john` | `User@123` | `USER` |

## API Surface

| Method | Path | Access |
|--------|------|--------|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public (proxied to Keycloak token endpoint) |
| POST | `/api/v1/auth/logout` | Authenticated |
| POST | `/api/v1/auth/refresh` | Public |
| GET | `/api/v1/users/me` | Authenticated |
| GET | `/api/v1/users` | `ROLE_ADMIN` |
| GET | `/api/v1/audit-logs` | `ROLE_ADMIN` |

## Database Migrations

Flyway migrations: `src/main/resources/db/migration/`. Only one migration (`V1`). Schema: `audit_logs` table only.

## Known Pitfalls

- **Jackson 3 (Spring Boot 4):** `spring.jackson.serialization.write-dates-as-timestamps=false` is **invalid** — Jackson 3 (`tools.jackson`) ignores this key and causes startup failure. No Jackson config is needed; `WRITE_DATES_AS_TIMESTAMPS=false` is already the default.
- **Realm JSON filename:** Keycloak `--import-realm` requires the file to be named `{realm-name}-realm.json` → `app-realm-realm.json`. Renaming it will break auto-import.
- **Port 3307:** MySQL is exposed on host port `3307` (not 3306) to avoid conflicts. The Docker `app` service connects internally on `3306`.

## Not Implemented (by design)

Swagger, unit/integration tests, CORS, HTTPS, rate limiting, Redis, Kafka, forgot-password, custom session management, custom JWT generation.
