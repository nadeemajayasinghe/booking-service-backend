package com.carRental.booking.repo;


import com.carRental.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerEmail(String customerEmail);

    List<Booking> findByVehicleId(Long vehicleId);

    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.vehicleId = ?1 AND b.status != 'CANCELLED' " +
            "AND ((b.pickupDate <= ?3 AND b.returnDate >= ?2))")
    List<Booking> findConflictingBookings(Long vehicleId, LocalDate pickupDate, LocalDate returnDate);

    List<Booking> findByPickupDateBetween(LocalDate startDate, LocalDate endDate);
}
