package com.vehiclerental.bookingservice.dto;

import com.vehiclerental.bookingservice.model.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
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
    private Integer numberOfDays;
    private BigDecimal pricePerDay;
    private BigDecimal totalAmount;
    private Booking.BookingStatus status;
    private String specialRequests;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookingResponse fromEntity(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setVehicleId(booking.getVehicleId());
        response.setVehicleName(booking.getVehicleName());
        response.setCustomerName(booking.getCustomerName());
        response.setCustomerEmail(booking.getCustomerEmail());
        response.setCustomerPhone(booking.getCustomerPhone());
        response.setLicenseNumber(booking.getLicenseNumber());
        response.setPickupDate(booking.getPickupDate());
        response.setReturnDate(booking.getReturnDate());
        response.setPickupLocation(booking.getPickupLocation());
        response.setReturnLocation(booking.getReturnLocation());
        response.setNumberOfDays(booking.getNumberOfDays());
        response.setPricePerDay(booking.getPricePerDay());
        response.setTotalAmount(booking.getTotalAmount());
        response.setStatus(booking.getStatus());
        response.setSpecialRequests(booking.getSpecialRequests());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }
}
