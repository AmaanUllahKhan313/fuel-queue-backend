# Fuel Queue - System Architecture

## Project Overview
Fuel Queue is a mobile application that helps users locate nearby fuel stations and track queue status in real-time using GPS and crowd-sourced data.

**Stack:** Spring Boot 3.2.5 (Backend) + Android (Frontend)  
**Authentication:** JWT-based OTP (Mobile Phone Number)  
**Database:** PostgreSQL (Production) / H2 (Testing)  
**Last Updated:** May 5, 2026

---

## System Architecture

### High-Level Architecture Diagram
```
┌─────────────────────────────────────────────────────────────┐
│                    Android Frontend UI                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │LoginFragment│  │MapFragment │  │StationList │            │
│  └────────────┘  └────────────┘  └────────────┘            │
└────────────────────────┬──────────────────────────────────────┘
                         │ REST API
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot Backend (Port 8080)                │
├─────────────────────────────────────────────────────────────┤
│                  Authentication Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │AuthController│  │OtpService    │  │JwtService    │      │
│  │/send-otp     │  │/verify-otp   │  │Token Gen/Val │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├─────────────────────────────────────────────────────────────┤
│                   Business Logic Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │StationService│  │CrowdService  │  │LocationPing  │      │
│  │Station CRUD  │  │Real-time Data│  │Service       │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├─────────────────────────────────────────────────────────────┤
│                   Data Access Layer                          │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │FuelStationRep│  │UserRepository│                         │
│  │              │  │              │                         │
│  └──────────────┘  └──────────────┘                         │
├─────────────────────────────────────────────────────────────┤
│                   Data Layer                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │PostgreSQL DB │  │H2 (Testing)  │  │CrowdStore    │      │
│  │FuelStation   │  │              │  │(In-Memory)   │      │
│  │User          │  │              │  │              │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

---

## Backend Architecture

### 1. Core Components

#### **Models** (`com.fuelqueue.model`)
| Entity | Purpose | Key Fields |
|--------|---------|-----------|
| `User` | User account & authentication | phoneNumber (unique), name, phoneVerified, otp, otpExpiresAt, fcmToken |
| `FuelStation` | Fuel station details | id, name, latitude, longitude, fuelType, pricePerLiter |
| `LocationPing` | GPS pings from users | userId, latitude, longitude, timestamp |

#### **DTOs** (`com.fuelqueue.dto`)
- `AuthRequest` - Phone number + OTP for authentication
- `OtpRequest` - Phone number to request OTP
- `OtpResponse` - Response after sending OTP
- `NearbyStationsResponse` - Stations within 5km radius
- `CrowdStatusResponse` - Crowd density at station

#### **Controllers** (`com.fuelqueue.controller`)
```
AuthController
├── POST /api/auth/send-otp
└── POST /api/auth/verify-otp

StationController
├── GET /api/stations/nearby
├── GET /api/stations/{id}
└── GET /api/stations/crowd-status/{stationId}

GpsController
└── POST /api/gps/ping
```

#### **Services** (`com.fuelqueue.service`)
- **OtpService** - Generate, store, validate OTPs (6-digit, 10-min expiry)
- **CrowdService** - Calculate crowd density using Haversine formula
- **StationService** - Manage fuel station data
- **CrowdStore** - In-memory crowd tracking (replaces Redis)

#### **Security** (`com.fuelqueue.security`)
- **JwtAuthFilter** - Validates JWT tokens on protected endpoints
- **JwtService** - Creates JWT tokens (30-day expiry), validates signatures
- **SecurityConfig** - Spring Security configuration, CORS setup

#### **Repositories** (`com.fuelqueue.repository`)
- **UserRepository** - DB access for users (findByPhoneNumber)
- **FuelStationRepository** - DB access for fuel stations

---

## Authentication Flow (OTP-Based)

### **Scenario 1: First-Time Registration**
```
User Phone Input
    ↓
POST /api/auth/send-otp { phoneNumber: "+919876543210" }
    ↓ [OtpService.generateOtp()]
Server stores OTP (6-digit, 10-min expiry)
Server returns: { message: "OTP sent", phoneNumber, otp* }
    ↓ [*For testing only]
User receives OTP via SMS (TODO: Integrate Twilio)
    ↓
POST /api/auth/verify-otp { phoneNumber, otp, name: "Rahul" }
    ↓
OtpService validates OTP & expiry
User created (phoneVerified=true)
JWT token generated: 30-day expiry
Response: { token, userId, name, phoneNumber }
    ↓
Client stores token in SharedPreferences
    ↓
Auth success → Navigate to Map
```

### **Scenario 2: Existing User Login**
```
POST /api/auth/send-otp { phoneNumber: "+919876543210" }
    ↓
User exists → Generate new OTP
Server stores OTP (overwrites previous)
    ↓
POST /api/auth/verify-otp { phoneNumber, otp }
    ↓
OtpService validates
JWT token generated
    ↓
Auth success → Navigate to Map
```

### **OTP Properties**
- **Format:** 6-digit random number
- **Validity:** 10 minutes
- **Storage:** Database (User.otp field)
- **Expiry Check:** LocalDateTime.now() > User.otpExpiresAt
- **SMS Delivery:** TODO - Integrate with Twilio / AWS SNS / Firebase

---

## API Endpoints

### Authentication
```
POST /api/auth/send-otp
├── Request: { "phoneNumber": "+919876543210" }
├── Response: { "message": "OTP sent", "phoneNumber": "+919876543210", "otp": "123456" }
└── Status: 200 OK / 400 Bad Request

POST /api/auth/verify-otp
├── Request: { "phoneNumber": "+919876543210", "otp": "123456", "name": "Rahul" }
├── Response: { "token": "eyJhbGc...", "userId": 1, "name": "Rahul", "phoneNumber": "+919876543210" }
└── Status: 200 OK / 401 Unauthorized
```

### Stations (Protected - Requires JWT)
```
GET /api/stations/nearby
├── Query Params: latitude, longitude, radiusKm (default 5)
├── Response: { "stations": [...], "totalCount": 10 }
└── Status: 200 OK

GET /api/stations/{id}
├── Response: FuelStation object
└── Status: 200 OK

GET /api/stations/crowd-status/{stationId}
├── Response: { "stationId": 1, "crowdLevel": "HIGH", "avgWaitTime": 45 }
└── Status: 200 OK
```

### GPS Tracking (Protected)
```
POST /api/gps/ping
├── Request: { "latitude": 28.6139, "longitude": 77.2090 }
├── Response: { "message": "Location updated" }
└── Status: 200 OK
```

---

## Database Schema

### users
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone_number VARCHAR(20) UNIQUE NOT NULL,
  phone_verified BOOLEAN DEFAULT false,
  otp VARCHAR(10),
  otp_expires_at TIMESTAMP,
  name VARCHAR(100),
  fcm_token VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### fuel_stations
```sql
CREATE TABLE fuel_stations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  latitude DECIMAL(10, 8) NOT NULL,
  longitude DECIMAL(11, 8) NOT NULL,
  fuel_type VARCHAR(50),
  price_per_liter DECIMAL(5, 2),
  is_open BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### location_pings
```sql
CREATE TABLE location_pings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL REFERENCES users(id),
  latitude DECIMAL(10, 8) NOT NULL,
  longitude DECIMAL(11, 8) NOT NULL,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Android Frontend Architecture

### **Layer Structure**
```
fuel-queue-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/fuelqueue/
│   │   │   ├── activity/
│   │   │   │   ├── LoginActivity / OtpAuthActivity
│   │   │   │   └── MainActivity
│   │   │   ├── fragment/
│   │   │   │   ├── LoginFragment [UPDATED]
│   │   │   │   ├── MapFragment
│   │   │   │   └── StationListFragment
│   │   │   ├── service/
│   │   │   │   ├── AuthService (Retrofit API)
│   │   │   │   └── StationService
│   │   │   ├── model/
│   │   │   │   ├── AuthRequest
│   │   │   │   ├── OtpRequest [NEW]
│   │   │   │   ├── OtpVerifyRequest [NEW]
│   │   │   │   ├── LoginResponse
│   │   │   │   └── FuelStation
│   │   │   ├── util/
│   │   │   │   ├── AuthInterceptor [NEW]
│   │   │   │   └── SharedPrefManager
│   │   │   └── viewmodel/
│   │   │       └── AuthViewModel
│   │   └── res/
│   │       └── layout/
│   │           ├── fragment_login.xml [UPDATED]
│   │           ├── fragment_map.xml
│   │           └── fragment_station_list.xml
│   └── build.gradle [UPDATED]
```

---

## UI Files Updated

### **Frontend Changes - Tracking**

| File | Status | Changes | Date |
|------|--------|---------|------|
| `fragment_login.xml` | ✅ UPDATED | Phone OTP flow, dynamic OTP section visibility | May 5, 2026 |
| `LoginFragment.java` | ✅ UPDATED | OTP send/verify flow, countdown timer | May 5, 2026 |
| `OtpRequest.java` | ✅ NEW | DTO for OTP request | May 5, 2026 |
| `OtpVerifyRequest.java` | ✅ NEW | DTO for OTP verification | May 5, 2026 |
| `OtpResponse.java` | ✅ NEW | DTO for OTP response | May 5, 2026 |
| `AuthService.java` | ✅ UPDATED | New sendOtp() and verifyOtp() endpoints | May 5, 2026 |
| `AuthInterceptor.java` | ✅ NEW | JWT token injection in requests | May 5, 2026 |
| `build.gradle` | ✅ UPDATED | Retrofit, OkHttp, Gson dependencies | May 5, 2026 |

---

## Backend Files Updated

### **Backend Changes - Tracking**

| File | Status | Changes | Date |
|------|--------|---------|------|
| `User.java` | ✅ UPDATED | phoneNumber, phoneVerified, otp, otpExpiresAt | May 4, 2026 |
| `OtpService.java` | ✅ NEW | OTP generation, validation, expiry logic | May 4, 2026 |
| `AuthController.java` | ✅ UPDATED | New /send-otp and /verify-otp endpoints | May 4, 2026 |
| `AuthRequest.java` | ✅ UPDATED | phoneNumber + otp instead of email+password | May 4, 2026 |
| `UserRepository.java` | ✅ UPDATED | findByPhoneNumber(), existsByPhoneNumber() | May 4, 2026 |
| `SecurityConfig.java` | ✅ UPDATED | JWT filter configuration | May 4, 2026 |
| `JwtService.java` | ✅ VERIFIED | Token generation/validation working | May 4, 2026 |
| `schema-postgres.sql` | ✅ UPDATED | users table with phone_number, otp fields | May 4, 2026 |
| `schema-h2.sql` | ✅ UPDATED | users table schema for testing | May 4, 2026 |
| `pom.xml` | ✅ VERIFIED | All Spring Boot 3.2.5 dependencies present | May 4, 2026 |

---

## Security Considerations

### ✅ Implemented
- [x] JWT token validation on protected endpoints
- [x] OTP expiry enforcement (10 minutes)
- [x] Phone number uniqueness constraint
- [x] CORS configuration for frontend

### ⚠️ TODO
- [ ] SMS delivery integration (Twilio / AWS SNS)
- [ ] OTP rate limiting (max 5 attempts/hour)
- [ ] Hash OTPs in database (currently plain text for testing)
- [ ] Add refresh token mechanism
- [ ] Implement logout endpoint with token blacklist
- [ ] Add request validation annotations (@Valid, @NotNull)
- [ ] Implement API rate limiting
- [ ] Add audit logging for authentication events

---

## Testing

### Running Tests
```bash
# Build and run all tests
mvn clean test

# Run specific test class
mvn test -Dtest=FuelQueueIntegrationTest

# Skip tests during build
mvn clean install -DskipTests
```

### Test Coverage
- ✅ `FuelQueueIntegrationTest.java` - API endpoint tests
- TODO: Add unit tests for services
- TODO: Add integration tests for OTP flow

---

## Deployment

### Prerequisites
- Java 17+ (Target Version)
- Maven 3.8+
- PostgreSQL 12+

### Build & Package
```bash
mvn clean package -DskipTests
# Creates: target/fuel-queue-backend-1.0.0.jar
```

### Run Locally
```bash
# Development profile
java -jar fuel-queue-backend-1.0.0.jar --spring.profiles.active=dev

# Production profile
java -jar fuel-queue-backend-1.0.0.jar --spring.profiles.active=prod
```

### Configuration Files
- `application-dev.properties` - Development (localhost, H2 DB)
- `application-local.properties` - Local testing
- `application-test.properties` - Unit tests
- `application-prod.properties` - Production (PostgreSQL)

---

## Troubleshooting

### Common Issues

**Issue:** OTP not being sent
- **Solution:** Implement SMS service (Twilio/AWS SNS) in `AuthController.sendOtp()`

**Issue:** Token validation failing
- **Solution:** Ensure `Authorization: Bearer <token>` header is sent with requests

**Issue:** Phone number validation errors
- **Solution:** Use `+91XXXXXXXXXX` format for India or implement country code selector

**Issue:** Database connection errors
- **Solution:** Verify PostgreSQL is running and `application-prod.properties` has correct credentials

---

## Future Roadmap

1. **SMS Integration** - Real OTP delivery via Twilio
2. **Firebase Push Notifications** - Real-time alerts for queue updates
3. **Payment Gateway** - In-app payment for premium features
4. **Admin Dashboard** - Station management and analytics
5. **Analytics** - Track user behavior and popular stations
6. **Multi-language Support** - Hindi, Tamil, etc.
7. **Offline Mode** - Cache station data locally
8. **Vehicle Management** - Track vehicle details and fuel consumption

