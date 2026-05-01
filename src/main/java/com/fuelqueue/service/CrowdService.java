package com.fuelqueue.service;

import com.fuelqueue.dto.CrowdStatusResponse;
import com.fuelqueue.dto.NearbyStationsResponse;
import com.fuelqueue.model.FuelStation;
import com.fuelqueue.model.LocationPing;
import com.fuelqueue.repository.FuelStationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CrowdService {

    @Value("${app.crowd.low-max:2}")
    private int lowMax;

    @Value("${app.crowd.medium-max:6}")
    private int mediumMax;

    @Value("${app.crowd.minutes-per-vehicle:3}")
    private int minutesPerVehicle;

    private final CrowdStore            crowdStore;
    private final FuelStationRepository stationRepo;

    public CrowdService(CrowdStore crowdStore, FuelStationRepository stationRepo) {
        this.crowdStore  = crowdStore;
        this.stationRepo = stationRepo;
    }

    // ── Process a GPS ping from the Android app ──────────────────────────────
    public Optional<CrowdStatusResponse> processLocationPing(LocationPing ping) {
        List<FuelStation> stations = stationRepo.findByActiveTrue();

        for (FuelStation station : stations) {
            double dist = haversineDistance(
                    ping.getLatitude(), ping.getLongitude(),
                    station.getLatitude(), station.getLongitude());

            if (dist <= station.getGeofenceRadiusMeters()) {
                crowdStore.recordPresence(station.getId(), ping.getUserId());
                return Optional.of(buildStatus(station));
            }
        }
        return Optional.empty(); // user not near any station
    }

    // ── Get crowd status for a specific station ──────────────────────────────
    public CrowdStatusResponse getCrowdStatus(FuelStation station) {
        return buildStatus(station);
    }

    // ── Get nearby stations with crowd info ──────────────────────────────────
    public List<NearbyStationsResponse> getNearby(double lat, double lng, double radiusMeters) {
        return stationRepo.findNearby(lat, lng, radiusMeters).stream()
                .map(s -> {
                    double dist  = haversineDistance(lat, lng, s.getLatitude(), s.getLongitude());
                    int    count = crowdStore.getActiveCount(s.getId());
                    return new NearbyStationsResponse(
                            s.getId(), s.getName(), s.getAddress(),
                            s.getLatitude(), s.getLongitude(),
                            Math.round(dist * 10.0) / 10.0,
                            crowdLevel(count), count,
                            estimateWait(count));
                }).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CrowdStatusResponse buildStatus(FuelStation station) {
        int    count = crowdStore.getActiveCount(station.getId());
        return new CrowdStatusResponse(
                station.getId(), station.getName(),
                count, crowdLevel(count), estimateWait(count));
    }

    public String crowdLevel(int count) {
        if (count <= lowMax)    return "LOW";
        if (count <= mediumMax) return "MEDIUM";
        return "HIGH";
    }

    public int estimateWait(int count) {
        return count * minutesPerVehicle;
    }

    /**
     * Haversine formula — accurate GPS distance in metres.
     */
    public static double haversineDistance(double lat1, double lng1,
                                           double lat2, double lng2) {
        final double R    = 6_371_000;
        double       dLat = Math.toRadians(lat2 - lat1);
        double       dLng = Math.toRadians(lng2 - lng1);
        double       a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                          + Math.cos(Math.toRadians(lat1))
                          * Math.cos(Math.toRadians(lat2))
                          * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
