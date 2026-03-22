package com.carRental.booking.repo;



import com.carRental.booking.entity.VehicleCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleCacheRepository extends JpaRepository<VehicleCache, String> {
    // JpaRepository gives you findById, save, deleteById, findAll — nothing extra needed
}