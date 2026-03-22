package com.carRental.booking.kafka;

import com.carRental.booking.entity.VehicleCache;
import com.carRental.booking.repo.VehicleCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VehicleCacheService {
    private final VehicleCacheRepository repo; // JPA repo for a VehicleCache entity

    public void upsert(VehicleEvent e) {
        VehicleCache v = repo.findById(e.getVehicleId()).orElse(new VehicleCache());
        v.setVehicleId(e.getVehicleId());
        v.setMake(e.getMake());
        v.setModel(e.getModel());
        v.setDailyRate(e.getDailyRate());
        v.setPlateNumber(e.getPlateNumber());
        v.setAvailable(true); // set false when a booking is created
        v.setUpdatedAt(e.getEventTimestamp());
        repo.save(v);
    }

    public void remove(String vehicleId) {
        repo.deleteById(vehicleId);
    }
}
