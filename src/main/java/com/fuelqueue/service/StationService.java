package com.fuelqueue.service;

import com.fuelqueue.dto.StationDetailsResponse;
import com.fuelqueue.model.FuelStation;
import com.fuelqueue.repository.FuelStationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    @Value("${app.crowd.minutes-per-vehicle:3}")
    private int minutesPerVehicle;

    private final FuelStationRepository repo;
    private final CrowdService crowdService;
    private final CrowdStore crowdStore;

    public StationService(FuelStationRepository repo, CrowdService crowdService, CrowdStore crowdStore) {
        this.repo = repo;
        this.crowdService = crowdService;
        this.crowdStore = crowdStore;
    }

    public FuelStation save(FuelStation station) {
        return repo.save(station);
    }

    public FuelStation findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found: " + id));
    }

    public List<FuelStation> findAll() {
        return repo.findAll();
    }

    public void delete(Long id) {
        FuelStation s = findById(id);
        s.setActive(false);
        repo.save(s);
    }

    /**
     * Get station details including stock availability.
     * Stock is considered available if at least one vehicle has been waiting for 3+ minutes.
     */
    public StationDetailsResponse getStationDetails(Long id) {
        FuelStation station = findById(id);
        
        // Get crowd information
        int crowdCount = crowdStore.getActiveCount(station.getId());
        String crowdLevel = crowdService.crowdLevel(crowdCount);
        int estimatedWait = crowdService.estimateWait(crowdCount);
        
        // Check stock availability (3 minutes threshold)
        boolean stockAvailable = crowdStore.isStockAvailable(station.getId(), 3);
        
        return new StationDetailsResponse(
                station.getId(),
                station.getName(),
                station.getAddress(),
                station.getLatitude(),
                station.getLongitude(),
                station.getGeofenceRadiusMeters(),
                station.isActive(),
                stockAvailable,
                station.isLive(),
                crowdCount,
                crowdLevel,
                estimatedWait
        );
    }
}
