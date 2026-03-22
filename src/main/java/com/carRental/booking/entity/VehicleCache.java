package com.carRental.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleCache {

    @Id
    @Column(name = "vehicle_id")
    private String vehicleId;

    private String make;
    private String brand;
    private String model;
    private int year;
    private String plateNumber;
    private BigDecimal dailyRate;
    private int mileage;
    private String color;
    private String imageUrl;
    private String description;
    private boolean available;
    private LocalDateTime updatedAt;
}