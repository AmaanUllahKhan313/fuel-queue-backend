package com.fuelqueue.controller;

import com.fuelqueue.dto.CrowdStatusResponse;
import com.fuelqueue.model.LocationPing;
import com.fuelqueue.service.CrowdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/gps")
public class GpsController {

    private final CrowdService crowdService;

    public GpsController(CrowdService crowdService) {
        this.crowdService = crowdService;
    }

    /**
     * POST /api/gps/ping
     * Android app sends this every ~10 seconds while navigating.
     *
     * Body: {
     *   "userId": 1,
     *   "latitude": 18.6298,
     *   "longitude": 73.7997,
     *   "speedKmh": 12.5,
     *   "timestamp": 1700000000000
     * }
     *
     * Response:
     *   - 200 with crowd status if user is inside a station geofence
     *   - 200 with { "message": "Not near any station" } otherwise
     */
    @PostMapping("/ping")
    public ResponseEntity<?> ping(@RequestBody LocationPing ping) {
        if (ping.getTimestamp() == 0) {
            ping.setTimestamp(System.currentTimeMillis());
        }

        Optional<CrowdStatusResponse> result = crowdService.processLocationPing(ping);

        return result
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(Map.of(
                        "message",   "Not near any station",
                        "latitude",  ping.getLatitude(),
                        "longitude", ping.getLongitude()
                )));
    }
}
