package com.carRental.booking.service;

import com.carRental.booking.dto.request.BookingRequest;
import com.vehiclerental.bookingservice.dto.BookingResponse;
import com.vehiclerental.bookingservice.model.Booking;
import com.carRental.booking.repo.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // Validate dates
        if (request.getReturnDate().isBefore(request.getPickupDate()) ||
                request.getReturnDate().isEqual(request.getPickupDate())) {
            throw new IllegalArgumentException("Return date must be after pickup date");
        }

        // Check for conflicting bookings
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                request.getVehicleId(),
                request.getPickupDate(),
                request.getReturnDate()
        );

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Vehicle is not available for the selected dates");
        }

        // Calculate number of days and total amount
        int numberOfDays = (int) ChronoUnit.DAYS.between(request.getPickupDate(), request.getReturnDate());
        BigDecimal totalAmount = request.getPricePerDay().multiply(new BigDecimal(numberOfDays));

        // Create booking entity
        Booking booking = new Booking();
        booking.setVehicleId(request.getVehicleId());
        booking.setVehicleName(request.getVehicleName());
        booking.setCustomerName(request.getCustomerName());
        booking.setCustomerEmail(request.getCustomerEmail());
        booking.setCustomerPhone(request.getCustomerPhone());
        booking.setLicenseNumber(request.getLicenseNumber());
        booking.setPickupDate(request.getPickupDate());
        booking.setReturnDate(request.getReturnDate());
        booking.setPickupLocation(request.getPickupLocation());
        booking.setReturnLocation(request.getReturnLocation());
        booking.setNumberOfDays(numberOfDays);
        booking.setPricePerDay(request.getPricePerDay());
        booking.setTotalAmount(totalAmount);
        booking.setStatus(request.getStatus() != null ? request.getStatus() : Booking.BookingStatus.PENDING);
        booking.setSpecialRequests(request.getSpecialRequests());

        Booking savedBooking = bookingRepository.save(booking);
        return BookingResponse.fromEntity(savedBooking);
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
        return BookingResponse.fromEntity(booking);
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByCustomerEmail(String email) {
        return bookingRepository.findByCustomerEmail(email).stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByVehicleId(Long vehicleId) {
        return bookingRepository.findByVehicleId(vehicleId).stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByStatus(Booking.BookingStatus status) {
        return bookingRepository.findByStatus(status).stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
        booking.setStatus(status);
        Booking updatedBooking = bookingRepository.save(booking);
        return BookingResponse.fromEntity(updatedBooking);
    }

    @Transactional
    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new RuntimeException("Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }

    public boolean isVehicleAvailable(Long vehicleId, LocalDate pickupDate, LocalDate returnDate) {
        List<Booking> conflicts = bookingRepository.findConflictingBookings(vehicleId, pickupDate, returnDate);
        return conflicts.isEmpty();
    }
}
