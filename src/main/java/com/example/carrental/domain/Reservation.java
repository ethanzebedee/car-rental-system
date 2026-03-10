package com.example.carrental.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a reservation in the car rental system.
 * A reservation is immutable once created and tracks a car rental for a
 * specific time period.
 */
@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    private String id;

    @Column(nullable = false)
    private String carId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarType carType;

    @Column(nullable = false)
    private LocalDateTime start;

    @Column(nullable = false)
    private int numberOfDays;

    // Default constructor for JPA
    protected Reservation() {
    }

    public Reservation(String carId, CarType carType,
            LocalDateTime start, int numberOfDays) {
        if (numberOfDays <= 0) {
            throw new IllegalArgumentException("Number of days must be greater than 0");
        }
        if (carId == null || carId.isBlank()) {
            throw new IllegalArgumentException("Car ID cannot be null or empty");
        }
        if (carType == null) {
            throw new IllegalArgumentException("Car type cannot be null");
        }
        if (start == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }

        this.id = UUID.randomUUID().toString();
        this.carId = carId;
        this.carType = carType;
        this.start = start;
        this.numberOfDays = numberOfDays;
    }

    public String getId() {
        return id;
    }

    public String getCarId() {
        return carId;
    }

    public CarType getCarType() {
        return carType;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public int getNumberOfDays() {
        return numberOfDays;
    }

    public LocalDateTime getEnd() {
        return start.plusDays(numberOfDays);
    }

    /**
     * Determines whether the time period covered by this reservation
     * overlaps with another booking window. Used by the service to
     * reject conflicting reservations.
     *
     * @param otherStart start of the other booking
     * @param otherDays  duration of the other booking in days
     * @return true if the intervals intersect (even by a partial day)
     */
    public boolean overlaps(LocalDateTime otherStart, int otherDays) {
        LocalDateTime otherEnd = otherStart.plusDays(otherDays);
        // overlap occurs when this.start < otherEnd AND this.end > otherStart
        return start.isBefore(otherEnd) && getEnd().isAfter(otherStart);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Reservation that = (Reservation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id='" + id + '\'' +
                ", carId='" + carId + '\'' +
                ", carType=" + carType +
                ", start=" + start +
                ", numberOfDays=" + numberOfDays +
                "}";
    }
}