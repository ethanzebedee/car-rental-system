package com.example.carrental.domain;

import java.util.UUID;

// TODO: Consider adding equals/hashCode and useful helper methods
// TODO: Add fields for additional metadata (license plate, status) if needed
public class Car {
    private final String id;
    private final CarType type;

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
}