package com.example.carrental.domain;

import java.time.LocalDateTime;

// TODO: Validate inputs (e.g., numberOfDays > 0) and consider adding getters for all fields
public class Reservation {
    private final String carId;
    private final CarType carType;
    private final LocalDateTime start;
    private final int numberOfDays;

    public Reservation(String carId, CarType carType,
            LocalDateTime start, int numberOfDays) {
        this.carId = carId;
        this.carType = carType;
        this.start = start;
        this.numberOfDays = numberOfDays;
    }

    public LocalDateTime getEnd() {
        return start.plusDays(numberOfDays);
    }

    public boolean overlaps(LocalDateTime otherStart, int otherDays) {
        LocalDateTime otherEnd = otherStart.plusDays(otherDays);
        return start.isBefore(otherEnd) && getEnd().isAfter(otherStart);
    }

    public String getCarId() {
        return carId;
    }
}