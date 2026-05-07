# AGENTS.md

## Project Snapshot
- Spring Boot 3.2 REST backend for fuel-station crowd estimation from GPS pings.
- Main entrypoint: `src/main/java/com/fuelqueue/FuelQueueApp.java`.
- Runtime stack: Spring Web, Spring Data JPA, Spring Security, JWT (`jjwt`), H2/Postgres.

## Architecture and Data Flow
- Request flow is `controller -> service -> repository` with DTO responses for crowd/station APIs.
- GPS path: `POST /api/gps/ping` (`GpsController`) -> `CrowdService.processLocationPing` -> geofence match via Haversine -> `CrowdStore.recordPresence` -> `CrowdStatusResponse`.
- Nearby path: `GET /api/stations/nearby` -> `CrowdService.getNearby` -> `FuelStationRepository.findNearby(...)` JPQL distance query + in-memory crowd overlay.
- Station details path: `GET /api/stations/{id}` -> `StationService.getStationDetails` combines DB station data and `CrowdStore` state (`stockAvailable` is derived, not persisted).
- Auth path is OTP-based now: `send-otp` + `verify-otp` in `AuthController`; `register` and `login` endpoints intentionally return HTTP 410.

## Security Boundaries (Important)
- `SecurityConfig` permits `/api/auth/**`, `/api/stations/**`, `/api/gps/**`, `/h2-console/**`, `/actuator/**`; many business endpoints are public by design right now.
- `JwtAuthFilter` only sets `SecurityContext` when `Authorization: Bearer ...` is present and valid; it does not load roles/authorities.
- JWT subject is currently a phone-number string (`JwtService.generateToken(user.getPhoneNumber(), user.getId())`).

## State and Persistence Conventions
- Persistent entities: `FuelStation`, `User`; `LocationPing` is request-only (there is a SQL table, but no JPA entity/repository usage).
- Crowd presence is in-memory only: `CrowdStore` (`ConcurrentHashMap`) with TTL (`app.crowd.user-ttl-seconds`, default 30s).
- Soft delete means `FuelStation.active=false` in `StationService.delete`; repository methods differ:
  - `findNearby` and `findByActiveTrue` respect active flag.
  - `StationService.findAll()` uses `repo.findAll()` (includes inactive stations).

## Profiles, Build, and Test Workflow
- Maven project targets Java 21 (`pom.xml` compiler `source/target=21`).
- Verified locally: `mvn test` fails on JDK <21 with `release version 21 not supported`.
- Integration suite: `src/test/java/com/fuelqueue/FuelQueueIntegrationTest.java` uses `@ActiveProfiles("test")` + MockMvc with ordered end-to-end API checks.
- Test data and schemas come from `src/main/resources/data.sql`, `schema-h2.sql`, `schema-postgres.sql`.
- Because there is no base `application.properties`, run with an explicit profile (for example `local`, `dev`, `qa`, `prod`, or `test`).

## Project-Specific Patterns to Follow
- Keep API response shape stable via DTO classes in `src/main/java/com/fuelqueue/dto` (do not return ad-hoc maps except existing auth/message cases).
- For distance/crowd features, reuse `CrowdService.haversineDistance`, `crowdLevel`, and `estimateWait` instead of duplicating formulas.
- Keep crowd thresholds/config externally controlled via `app.crowd.*` properties; avoid hardcoding except where already present (for example stock threshold `3` minutes in `StationService`).
- If changing auth, preserve deprecation behavior for `/api/auth/register` and `/api/auth/login` unless API contract is intentionally migrated.
- Seed stations in `data.sql` are used by tests and manual API checks; avoid breaking IDs/coordinates assumptions without updating tests.

