package com.fuelqueue.service;

import com.fuelqueue.model.FuelStation;
import com.fuelqueue.repository.FuelStationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    private final FuelStationRepository repo;

    public StationService(FuelStationRepository repo) {
        this.repo = repo;
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
}
