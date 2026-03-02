package com.example.carrental.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a car in the rental system
 * Each car has a unique ID and a specific type
 * In a real system the car could also have a license plate, mileage,
 * or other attributes
 */
public class Car {
    private final String id; // unique identifier for this car instance
    private final CarType type; // the category of vehicle

    public Car(CarType type) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public CarType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Car car = (Car) o;
        return Objects.equals(id, car.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Car{" +
                "id='" + id + '\'' +
                ", type=" + type +
                '}';
    }
}