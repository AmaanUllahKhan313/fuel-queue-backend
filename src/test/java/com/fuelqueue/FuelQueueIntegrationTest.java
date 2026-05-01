package com.fuelqueue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuelqueue.model.FuelStation;
import com.fuelqueue.model.LocationPing;
import com.fuelqueue.dto.AuthRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FuelQueueIntegrationTest {

    @Autowired MockMvc     mvc;
    @Autowired ObjectMapper mapper;

    static String authToken;
    static Long   testStationId;

    // ── Auth Tests ────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("POST /api/auth/register — registers a new user")
    void testRegister() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setEmail("test@fuelqueue.com");
        req.setPassword("password123");
        req.setName("Test User");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Registered successfully"));
    }

    @Test @Order(2)
    @DisplayName("POST /api/auth/register — duplicate email returns 400")
    void testRegisterDuplicate() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setEmail("test@fuelqueue.com");
        req.setPassword("password123");
        req.setName("Test User");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test @Order(3)
    @DisplayName("POST /api/auth/login — returns JWT token")
    void testLogin() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setEmail("test@fuelqueue.com");
        req.setPassword("password123");

        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.userId").isNumber())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        authToken = mapper.readTree(body).get("token").asText();
    }

    @Test @Order(4)
    @DisplayName("POST /api/auth/login — wrong password returns 401")
    void testLoginWrongPassword() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setEmail("test@fuelqueue.com");
        req.setPassword("wrongpassword");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }

    // ── Station Tests ─────────────────────────────────────────────────────────

    @Test @Order(5)
    @DisplayName("GET /api/stations — returns seeded stations")
    void testListStations() throws Exception {
        mvc.perform(get("/api/stations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }

    @Test @Order(6)
    @DisplayName("POST /api/stations — adds a new station")
    void testCreateStation() throws Exception {
        FuelStation station = new FuelStation(
                "Test HP Pump", "Test Road, Pune", 18.6350, 73.8020, 80.0);

        MvcResult result = mvc.perform(post("/api/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(station)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("Test HP Pump"))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        testStationId = mapper.readTree(body).get("id").asLong();
    }

    @Test @Order(7)
    @DisplayName("GET /api/stations/{id} — returns created station")
    void testGetStation() throws Exception {
        mvc.perform(get("/api/stations/" + testStationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test HP Pump"))
            .andExpect(jsonPath("$.latitude").value(18.6350));
    }

    @Test @Order(8)
    @DisplayName("GET /api/stations/999 — returns 404 for unknown station")
    void testGetStationNotFound() throws Exception {
        mvc.perform(get("/api/stations/999999"))
            .andExpect(status().isNotFound());
    }

    @Test @Order(9)
    @DisplayName("GET /api/stations/nearby — returns stations within radius")
    void testNearbyStations() throws Exception {
        // Pimpri coords — should find seeded stations
        mvc.perform(get("/api/stations/nearby")
                .param("lat", "18.6298")
                .param("lng", "73.7997")
                .param("radius", "10000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(greaterThan(0)))
            .andExpect(jsonPath("$[0].crowdLevel", oneOf("LOW", "MEDIUM", "HIGH")))
            .andExpect(jsonPath("$[0].estimatedWaitMinutes").isNumber());
    }

    @Test @Order(10)
    @DisplayName("GET /api/stations/{id}/crowd — returns crowd status")
    void testCrowdStatus() throws Exception {
        mvc.perform(get("/api/stations/1/crowd"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stationId").value(1))
            .andExpect(jsonPath("$.crowdLevel", oneOf("LOW", "MEDIUM", "HIGH")))
            .andExpect(jsonPath("$.activeUsers").isNumber())
            .andExpect(jsonPath("$.estimatedWaitMinutes").isNumber());
    }

    @Test @Order(11)
    @DisplayName("PUT /api/stations/{id} — updates station name")
    void testUpdateStation() throws Exception {
        FuelStation updated = new FuelStation(
                "Updated HP Pump", "New Address, Pune", 18.6350, 73.8020, 100.0);

        mvc.perform(put("/api/stations/" + testStationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(updated)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated HP Pump"))
            .andExpect(jsonPath("$.geofenceRadiusMeters").value(100.0));
    }

    // ── GPS Ping Tests ────────────────────────────────────────────────────────

    @Test @Order(12)
    @DisplayName("POST /api/gps/ping — not near any station returns message")
    void testPingNotNearStation() throws Exception {
        LocationPing ping = new LocationPing(1L, 0.0, 0.0, 0.0, System.currentTimeMillis());

        mvc.perform(post("/api/gps/ping")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(ping)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Not near any station"));
    }

    @Test @Order(13)
    @DisplayName("POST /api/gps/ping — inside geofence returns crowd status")
    void testPingInsideGeofence() throws Exception {
        // Exact coords of seeded HP Petrol Pump Pimpri — inside geofence
        LocationPing ping = new LocationPing(
                1L, 18.6298, 73.7997, 5.0, System.currentTimeMillis());

        mvc.perform(post("/api/gps/ping")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(ping)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stationName").isString())
            .andExpect(jsonPath("$.crowdLevel", oneOf("LOW", "MEDIUM", "HIGH")))
            .andExpect(jsonPath("$.activeUsers").value(greaterThanOrEqualTo(1)));
    }

    @Test @Order(14)
    @DisplayName("POST /api/gps/ping — multiple users increase crowd count")
    void testPingMultipleUsersIncreaseCrowd() throws Exception {
        long t = System.currentTimeMillis();
        // 5 different users ping the same station
        for (long userId = 10L; userId <= 14L; userId++) {
            LocationPing ping = new LocationPing(
                    userId, 18.6298, 73.7997, 2.0, t);
            mvc.perform(post("/api/gps/ping")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(ping)))
                .andExpect(status().isOk());
        }

        // Crowd should now be HIGH (7+ users including earlier tests)
        mvc.perform(get("/api/stations/1/crowd"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeUsers").value(greaterThanOrEqualTo(5)));
    }

    @Test @Order(15)
    @DisplayName("DELETE /api/stations/{id} — soft deletes the station")
    void testDeleteStation() throws Exception {
        mvc.perform(delete("/api/stations/" + testStationId))
            .andExpect(status().isNoContent());
    }
}
