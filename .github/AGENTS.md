# AGENTS.md — Fuel Queue Backend

Real-time fuel station crowd tracking API (Spring Boot 3.2, Java 17). This guide helps AI agents quickly understand the architecture and contribute effectively.

---

## 🎯 Core Purpose

**Real-time GPS geofencing system** that detects when users are near fuel stations and calculates live crowd levels + estimated wait times.

### Data Flow
```
Android App (GPS ping every 10s) 
  → /api/gps/ping 
  → CrowdService.processLocationPing()
        (checks Haversine distance to all active stations)
  → CrowdStore.recordPresence() (in-memory TTL store, 30s default)
  → CrowdStatusResponse (crowd level: LOW/MEDIUM/HIGH + wait time in minutes)
```

---

## 🏗️ Architecture & Key Components

### **CrowdService** — Core Business Logic
- **Location**: `service/CrowdService.java`
- **Responsibility**: GPS distance calculations, crowd level mapping, wait time estimation
- **Key method**: `processLocationPing(LocationPing ping)` 
  - Iterates through **active stations only** (`findByActiveTrue()`)
  - Uses **Haversine formula** (lines 93–103) for accurate GPS distance in meters
  - Records user presence if distance ≤ station's `geofenceRadiusMeters` (default 80m)
  - Returns crowd status or empty if not near any station
- **Config-driven thresholds** (injected from `application.properties`):
  - `app.crowd.low-max=2` (≤2 users = LOW)
  - `app.crowd.medium-max=6` (≤6 users = MEDIUM)
  - `app.crowd.minutes-per-vehicle=3` (wait time = count × 3 min)

### **CrowdStore** — In-Memory User Presence (Redis Replacement)
- **Location**: `service/CrowdStore.java`
- **Pattern**: `Map<stationId, Map<userId, timestamp>>` with TTL-based eviction
- **Key methods**:
  - `recordPresence()` — records user at station with current timestamp
  - `getActiveCount()` — counts users seen within TTL window (filters by `System.currentTimeMillis() - ttlSeconds*1000`)
  - `evictStale()` — manual cleanup (can be called periodically)
- **Critical detail**: Uses **ConcurrentHashMap** for thread-safe concurrent pings
- **TTL logic**: Users auto-expire after 30s of inactivity (simulates Redis TTL)

### **FuelStationRepository** — Custom Geo-Queries
- **Location**: `repository/FuelStationRepository.java`
- **Key query**: `findNearby(lat, lng, radiusMeters)` 
  - Embeds **Haversine formula directly in JPQL** (lines 18–26)
  - Works with H2 in-memory DB (trigonometric functions supported)
  - Returns only `active = true` stations

### **JwtService** — Stateless Authentication
- **Location**: `security/JwtService.java`
- **Pattern**: Stateless JWT (no session storage needed for H2)
- **Token claims**: `email` (subject) + `userId` (custom claim)
- **Validation**: Uses JJWT library v0.12.3 with HMAC-SHA256
- **Error handling**: Catches `JwtException` and returns boolean (no exceptions to caller)

### **SecurityConfig** — Public vs. Protected Routes
- **Location**: `config/SecurityConfig.java`
- **Design choice**: All endpoints in `/api/auth/**`, `/api/stations/**`, `/api/gps/**` are **permit-all** (stateless mobile app design)
- **JWT filter injected before** `UsernamePasswordAuthenticationFilter` (line 44)
- **Session policy**: `STATELESS` (no cookies, no sessions)
- **H2 console**: Frame options set to `sameOrigin` (line 43)

---

## ⚙️ Build & Test Workflows

### **Build (Terminal)**
```bash
mvn clean compile     # Compile only
mvn clean package     # Full build + JAR in target/
mvn clean install     # Local Maven cache
```

### **Run (Local)**
```bash
# Terminal
mvn spring-boot:run

# IntelliJ: Open FuelQueueApp.java → Click ▶ Run button
# (IntelliJ auto-finds Maven and runs spring-boot:run goal)

# Server at http://localhost:8080
# H2 Console: http://localhost:8080/h2-console (creds: sa / empty password)
```

### **Tests**
```bash
mvn test                    # Run all 15 integration tests
# IntelliJ: Right-click FuelQueueIntegrationTest.java → Run Tests

# Test profile: @ActiveProfiles("test") uses application-test.properties
# Tests use @SpringBootTest + @AutoConfigureMockMvc (MockMvc, not real HTTP)
# MockMvc patterns: mvc.perform(get/post/put/delete(...)).andExpect(...)
```

### **Test Organization** (`FuelQueueIntegrationTest.java`)
- **Pattern**: `@Order` annotations dictate test sequence (e.g., register user first, then authenticate)
- **Modules**: Auth tests → Station tests → GPS ping tests → Crowd detection tests
- **Assertions**: Hamcrest matchers (`hasSize()`, `notNullValue()`, `closeTo()` for floats)
- **Token reuse**: Static `authToken` and `testStationId` shared across ordered tests

---

## 📋 Project Conventions & Patterns

### **Dependency Injection**
- Constructor injection **only** (no `@Autowired` fields)
- Example: `CrowdService(CrowdStore crowdStore, FuelStationRepository stationRepo)`
- Enables testability and immutability

### **@Value Configuration Properties**
- All tunable thresholds injected from `application.properties`:
  - Crowd levels, JWT secret, TTL, etc.
- **Fallback defaults**: `@Value("${app.crowd.low-max:2}")` (`:2` is default)
- Configuration file: `src/main/resources/application.properties`

### **URL Routing Convention**
- `/api/auth/*` — Authentication (register, login)
- `/api/stations/*` — Station CRUD + nearby query
- `/api/gps/*` — GPS pings from mobile app
- No `/admin/` or versioning endpoints (single API surface)

### **Response DTOs** (all in `dto/`)
- `CrowdStatusResponse` — Returned by GPS ping and crowd endpoint
- `NearbyStationsResponse` — List format with distance + crowd level
- `AuthRequest` — Request body for register/login (no response DTO, inline maps)

### **Soft Delete Pattern**
- Stations have `boolean active` column (not actual deletion)
- Every query filters `WHERE active = true`
- Allows historical tracking without data loss

---

## 🔑 Critical Business Rules

1. **Distance calculation**: **Always use Haversine formula** (both in Java and SQL)
   - Embedded in `CrowdService.haversineDistance()` (static method)
   - Also in SQL via `FuelStationRepository.findNearby()`
   - Earth radius: 6,371,000 meters (R = 6_371_000)

2. **Crowd presence TTL**: Users auto-expire after 30 seconds of no ping
   - Logic: `timestamp >= System.currentTimeMillis() - (ttlSeconds * 1000)`
   - Allows mobile app to go offline/lose connection without manual logout

3. **Crowd levels are threshold-based**:
   - `count ≤ 2` → LOW
   - `count ≤ 6` → MEDIUM
   - `count > 6` → HIGH
   - Any threshold change requires modifying `application.properties` (not hardcoded)

4. **Wait time formula**: `activeUserCount × minutesPerVehicle` (default: count × 3 min)
   - Simple linear model; can be replaced with real queue estimation

---

## 🗄️ Database & Storage

### **H2 In-Memory Database**
- **Scope**: `spring.jpa.hibernate.ddl-auto=create-drop` (recreates schema on startup)
- **Data initialization**: `src/main/resources/data.sql` (executed by Spring)
- **Tables**: `users`, `fuel_stations`, `location_pings` (Hibernate generates from entities)
- **Coordinates**: Stored as `DOUBLE` (latitude/longitude)

### **CrowdStore** (not persistent)
- Pure Java `ConcurrentHashMap` (volatile, cleared on app restart)
- **Prod upgrade**: Replace with Redis (same interface)
- No database writes needed; transient session data

---

## 🔐 Security & Auth Flow

### **JWT Token Lifecycle**
1. **Register** (`/api/auth/register`) → User created with bcrypt password
2. **Login** (`/api/auth/login`) → Validate credentials, return JWT token
3. **Subsequent requests** → Include `Authorization: Bearer <token>` header
4. **JwtAuthFilter** → Extract token, validate, populate `SecurityContext` (or permit public routes)

### **Token Claims**
- `subject` (iss) → User email
- `userId` (custom) → Long ID (needed for GPS pings)
- `issuedAt` → Issued timestamp
- `expiration` → 24 hours from issue (`app.jwt.expiration-ms=86400000`)

### **Static Secret Key** (Development Only)
- `app.jwt.secret=FuelQueueSuperSecretKeyForJWTSigning2024XYZ` in `application.properties`
- ⚠️ **Prod upgrade**: Use environment variables or Key Vault; rotate keys

---

## 📝 Common Editing Patterns

### **Add a new config property**:
1. Add to `application.properties`: `app.myfeature.threshold=100`
2. Inject in service: `@Value("${app.myfeature.threshold:100}") private int threshold;`
3. Use in logic
4. Add to `application-test.properties` for test consistency

### **Add a new API endpoint**:
1. Create controller method in appropriate `*Controller.java`
2. Add `@Get/Post/PutMapping("/path")`
3. Inject required services via constructor
4. Return response DTO or `ResponseEntity<>`
5. Add integration test in `FuelQueueIntegrationTest.java` with `@Order` sequence

### **Modify Haversine distance behavior**:
- Update `CrowdService.haversineDistance()` static method
- Also update embedded SQL in `FuelStationRepository.findNearby()` query
- **Both must be in sync** for consistency

### **Update JPA query**:
- Edit `@Query` in `*Repository.java` interface
- Use JPQL (not SQL) for H2 portability
- Test with `mvn test` to verify parsing

---

## 🚀 Prod Upgrade Path

| Component | Dev (Current) | Production |
|-----------|------|------------|
| Database | H2 in-memory | PostgreSQL (JDBC driver change) |
| Presence store | ConcurrentHashMap | Redis (keep same interface) |
| JWT secret | Static string | AWS Secrets Manager / Azure Key Vault |
| Session | Stateless (no storage) | Same + optional refresh tokens |
| Deployment | Local Tomcat | Docker container + K8s |

---

## 📂 File Organization (Quick Reference)

```
src/main/java/com/fuelqueue/
  ├── FuelQueueApp.java           ← Entry point (@SpringBootApplication)
  ├── config/SecurityConfig.java  ← Security & JWT filter chain
  ├── security/
  │   ├── JwtService.java         ← Token generation/validation
  │   └── JwtAuthFilter.java      ← Per-request JWT extraction
  ├── model/
  │   ├── FuelStation.java        ← @Entity, Haversine geofence
  │   ├── User.java               ← @Entity, bcrypt password
  │   └── LocationPing.java       ← Request DTO (not persisted)
  ├── dto/
  │   ├── CrowdStatusResponse.java
  │   ├── NearbyStationsResponse.java
  │   └── AuthRequest.java
  ├── repository/
  │   ├── FuelStationRepository.java  ← findNearby() with Haversine SQL
  │   └── UserRepository.java
  ├── service/
  │   ├── CrowdService.java        ← Core logic: Haversine + crowd calc
  │   ├── CrowdStore.java          ← In-memory TTL presence
  │   └── StationService.java      ← CRUD operations
  └── controller/
      ├── AuthController.java      ← /api/auth/register, /api/auth/login
      ├── StationController.java   ← /api/stations/*, /api/stations/nearby
      └── GpsController.java       ← /api/gps/ping

src/main/resources/
  ├── application.properties       ← Main config (H2, JWT, thresholds)
  ├── application-test.properties  ← Test profile overrides
  └── data.sql                     ← Initial seed data (6 Pune stations)

src/test/java/com/fuelqueue/
  └── FuelQueueIntegrationTest.java ← 15 ordered MockMvc tests
```

---

## 💡 Tips for AI Agents

1. **Always sync Haversine implementations** (Java method + SQL query)
2. **Use constructor injection** for all dependencies (DI container handles it)
3. **TTL filtering happens at read time**, not cleanup (lazy expiration pattern)
4. **Config properties are injected once at startup** — no runtime changes without restart
5. **H2 triggers and advanced SQL may differ from prod DB**; test integration tests before prod deployment
6. **All endpoints are public by design** (mobile app stateless auth); do not add role-based access control without updating SecurityConfig
7. **MockMvc tests must maintain @Order sequence** (shared static state; register user → login → use token)
8. **Test profiles use `application-test.properties`**; keep consistent with main properties but can override for test speed


