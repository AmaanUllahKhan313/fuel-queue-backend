package com.fuelqueue.controller;

import com.fuelqueue.dto.CrowdStatusResponse;
import com.fuelqueue.dto.NearbyStationsResponse;
import com.fuelqueue.dto.StationDetailsResponse;
import com.fuelqueue.model.FuelStation;
import com.fuelqueue.service.CrowdService;
import com.fuelqueue.service.StationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;
    private final CrowdService   crowdService;

    public StationController(StationService stationService, CrowdService crowdService) {
        this.stationService = stationService;
        this.crowdService   = crowdService;
    }

    /**
     * GET /api/stations
     * List all stations.
     */
    @GetMapping
    public ResponseEntity<List<FuelStation>> listAll() {
        return ResponseEntity.ok(stationService.findAll());
    }

    /**
     * GET /api/stations/nearby?lat=18.63&lng=73.80&radius=5000
     * Find stations within radius (metres) with live crowd info.
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyStationsResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") double radius) {
        return ResponseEntity.ok(crowdService.getNearby(lat, lng, radius));
    }

    /**
     * GET /api/stations/{id}
     * Get single station details with stock availability status.
     * Stock is available if at least one vehicle has been waiting for 3+ minutes.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StationDetailsResponse> getStation(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(stationService.getStationDetails(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/stations/{id}/crowd
     * Get current crowd level for a station.
     */
    @GetMapping("/{id}/crowd")
    public ResponseEntity<CrowdStatusResponse> getCrowd(@PathVariable Long id) {
        try {
            FuelStation station = stationService.findById(id);
            return ResponseEntity.ok(crowdService.getCrowdStatus(station));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/stations
     * Add a new fuel station.
     * Body: { "name": "HP Pump", "address": "...", "latitude": 18.63, "longitude": 73.80 }
     */
    @PostMapping
    public ResponseEntity<FuelStation> create(@RequestBody FuelStation station) {
        return ResponseEntity.ok(stationService.save(station));
    }

    /**
     * PUT /api/stations/{id}
     * Update station details.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FuelStation> update(@PathVariable Long id,
                                              @RequestBody FuelStation updated) {
        try {
            FuelStation existing = stationService.findById(id);
            existing.setName(updated.getName());
            existing.setAddress(updated.getAddress());
            existing.setLatitude(updated.getLatitude());
            existing.setLongitude(updated.getLongitude());
            existing.setGeofenceRadiusMeters(updated.getGeofenceRadiusMeters());
            return ResponseEntity.ok(stationService.save(existing));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/stations/{id}
     * Soft-delete (marks inactive).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            stationService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
