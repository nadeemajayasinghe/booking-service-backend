package com.carRental.booking.dto.request;


import com.carRental.booking.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    private Long vehicleId;
    private String vehicleName;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String licenseNumber;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private String pickupLocation;
    private String returnLocation;
    private BigDecimal pricePerDay;
    private String specialRequests;
    private Booking.BookingStatus status;
}
