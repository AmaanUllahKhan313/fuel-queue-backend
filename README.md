# ⛽ Fuel Queue Backend

Real-time fuel station crowd tracker REST API built with Spring Boot.
Uses **GPS geofencing** to detect how many users are at each station
and reports crowd levels (LOW / MEDIUM / HIGH) and estimated wait times.

---

## ✅ Prerequisites

| Tool     | Version  | Notes                        |
|----------|----------|------------------------------|
| Java     | 17+      | Required                     |
| Maven    | 3.8+     | Or use the IntelliJ Maven tab |
| IntelliJ | Any      | Community Edition works fine |

> **No Redis, no PostgreSQL, no Docker needed.**
> Uses H2 in-memory database — starts instantly.

---

## 🚀 Run in IntelliJ

1. Open IntelliJ → **File → Open** → select `fuel-queue-backend` folder
2. Wait for Maven to download dependencies (~1 min first time)
3. Open `src/main/java/com/fuelqueue/FuelQueueApp.java`
4. Click the ▶ green **Run** button
5. Server starts at **http://localhost:8080**

---

## 🧪 Run Tests

### IntelliJ
Right-click `src/test/java/com/fuelqueue/FuelQueueIntegrationTest.java` → **Run Tests**

### Terminal
```bash
mvn test
```

15 integration tests covering auth, stations, GPS pings, and crowd logic.

---

## 🌐 API Endpoints

### Auth
| Method | URL                  | Body                              | Description        |
|--------|----------------------|-----------------------------------|--------------------|
| POST   | `/api/auth/register` | `{email, password, name}`         | Register user      |
| POST   | `/api/auth/login`    | `{email, password}`               | Login → JWT token  |

### Stations
| Method | URL                        | Description                         |
|--------|----------------------------|-------------------------------------|
| GET    | `/api/stations`            | List all stations                   |
| POST   | `/api/stations`            | Add a new station                   |
| GET    | `/api/stations/{id}`       | Get station by ID                   |
| PUT    | `/api/stations/{id}`       | Update station                      |
| DELETE | `/api/stations/{id}`       | Soft-delete station                 |
| GET    | `/api/stations/nearby`     | Stations near GPS coords with crowd |
| GET    | `/api/stations/{id}/crowd` | Live crowd level for a station      |

### GPS Tracking
| Method | URL            | Description                                    |
|--------|----------------|------------------------------------------------|
| POST   | `/api/gps/ping`| Send user location — returns crowd if at station|

---

## 🗺️ Try It with curl

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"rahul@example.com","password":"pass123","name":"Rahul"}'

# 2. Login — copy the token from response
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rahul@example.com","password":"pass123"}'

# 3. List all stations (6 pre-seeded in Pune)
curl http://localhost:8080/api/stations

# 4. Stations near Pimpri with crowd info
curl "http://localhost:8080/api/stations/nearby?lat=18.6298&lng=73.7997&radius=10000"

# 5. Send GPS ping (simulates Android app)
curl -X POST http://localhost:8080/api/gps/ping \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"latitude":18.6298,"longitude":73.7997,"speedKmh":5.0,"timestamp":0}'

# 6. Check crowd at station 1
curl http://localhost:8080/api/stations/1/crowd
```

---

## 🏗️ How Crowd Detection Works

1. **Android app** sends GPS pings every ~10 seconds
2. **Backend** checks if the user's coordinates are within any station's **geofence** (80 m radius)
3. If inside, the user is recorded in an **in-memory store** with a 30-second TTL
4. **Crowd level** is calculated from active user count:
   - `0–2 users` → **LOW** (go now!)
   - `3–6 users` → **MEDIUM** (short wait)
   - `7+ users`  → **HIGH** (avoid if possible)
5. **Wait time** = activeUsers × 3 minutes

---

## 🗄️ H2 Console (view DB in browser)

Open: **http://localhost:8080/h2-console**

| Field    | Value                  |
|----------|------------------------|
| JDBC URL | `jdbc:h2:mem:fuelqueue`|
| Username | `sa`                   |
| Password | *(leave empty)*        |

---

## 📁 Project Structure

```
src/
├── main/java/com/fuelqueue/
│   ├── FuelQueueApp.java               ← Entry point
│   ├── config/
│   │   └── SecurityConfig.java         ← Spring Security + JWT setup
│   ├── security/
│   │   ├── JwtService.java             ← Token generate/validate
│   │   └── JwtAuthFilter.java          ← Per-request JWT check
│   ├── model/
│   │   ├── FuelStation.java            ← DB entity
│   │   ├── User.java                   ← DB entity
│   │   └── LocationPing.java           ← Request model
│   ├── dto/
│   │   ├── CrowdStatusResponse.java
│   │   ├── NearbyStationsResponse.java
│   │   └── AuthRequest.java
│   ├── repository/
│   │   ├── FuelStationRepository.java  ← JPA + nearby query
│   │   └── UserRepository.java
│   ├── service/
│   │   ├── CrowdStore.java             ← In-memory user presence (Redis replacement)
│   │   ├── CrowdService.java           ← Core crowd logic + Haversine
│   │   └── StationService.java
│   └── controller/
│       ├── AuthController.java
│       ├── StationController.java
│       └── GpsController.java
└── test/java/com/fuelqueue/
    └── FuelQueueIntegrationTest.java   ← 15 MockMvc tests
```

---

## 🔮 Production Upgrades

| Feature        | Current (Dev)         | Production              |
|----------------|-----------------------|-------------------------|
| Database        | H2 in-memory          | PostgreSQL               |
| Presence store  | ConcurrentHashMap     | Redis (with TTL)         |
| Notifications   | Not included          | Firebase FCM             |
| Auth            | JWT (stateless)       | Same + refresh tokens    |
| Deployment      | Local Tomcat          | Docker + cloud           |
