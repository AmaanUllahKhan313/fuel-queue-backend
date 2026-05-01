package com.fuelqueue.repository;

import com.fuelqueue.model.FuelStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FuelStationRepository extends JpaRepository<FuelStation, Long> {

    List<FuelStation> findByActiveTrue();

    /**
     * Find all active stations within a given radius (metres) using
     * the Haversine formula in JPQL (works with H2).
     */
    @Query("""
            SELECT s FROM FuelStation s
            WHERE s.active = true
            AND (6371000 * 2 * ASIN(SQRT(
                POWER(SIN((RADIANS(s.latitude)  - RADIANS(:lat))  / 2), 2) +
                COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
                POWER(SIN((RADIANS(s.longitude) - RADIANS(:lng)) / 2), 2)
            ))) <= :radiusMeters
            """)
    List<FuelStation> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters
    );
}
