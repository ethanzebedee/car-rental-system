package com.example.carrental.service;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.exception.NoAvailableCarException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service for managing car reservations in the rental system.
 * Handles reservation creation, cancellation, and availability queries.
 * Thread-safe implementation using CopyOnWriteArrayList.
 */
public class CarRentalService {

    private final List<Car> cars;
    private final List<Reservation> reservations;

    public CarRentalService(Map<CarType, Integer> initialInventory) {
        this.cars = new CopyOnWriteArrayList<>();
        this.reservations = new CopyOnWriteArrayList<>();

        initialInventory.forEach((type, count) -> {
            for (int i = 0; i < count; i++) {
                cars.add(new Car(type));
            }
        });
    }

    /**
     * Reserves a car of the specified type for the given dates.
     * 
     * @param type  the type of car to reserve
     * @param start the start time of the reservation
     * @param days  the number of days for the reservation
     * @return the created Reservation
     * @throws NoAvailableCarException  if no cars of the requested type are
     *                                  available
     * @throws IllegalArgumentException if parameters are invalid
     */
    public Reservation reserveCar(CarType type,
            LocalDateTime start,
            int days) {
        if (type == null) {
            throw new IllegalArgumentException("Car type cannot be null");
        }
        if (start == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Number of days must be greater than 0");
        }

        Car selectedCar = cars.stream()
                .filter(car -> car.getType() == type)
                .filter(car -> isAvailable(car.getId(), start, days))
                .findFirst()
                .orElseThrow(() -> new NoAvailableCarException(
                        "No " + type + " cars available from " + start + " for " + days + " days"));

        Reservation reservation = new Reservation(selectedCar.getId(), type, start, days);
        reservations.add(reservation);
        return reservation;
    }

    /**
     * Reserves a car (returns Optional for backward compatibility).
     * 
     * @param type  the type of car to reserve
     * @param start the start time of the reservation
     * @param days  the number of days for the reservation
     * @return an Optional containing the reservation if successful, empty otherwise
     */
    public Optional<Reservation> reserveCarOptional(CarType type,
            LocalDateTime start,
            int days) {
        try {
            return Optional.of(reserveCar(type, start, days));
        } catch (NoAvailableCarException e) {
            return Optional.empty();
        }
    }

    /**
     * Cancels an existing reservation.
     * 
     * @param reservationId the ID of the reservation to cancel
     * @return true if the reservation was found and cancelled, false otherwise
     */
    public boolean cancelReservation(String reservationId) {
        return reservations.removeIf(r -> r.getId().equals(reservationId));
    }

    /**
     * Retrieves all reservations for a specific car.
     * 
     * @param carId the ID of the car
     * @return a list of all reservations for the car
     */
    public List<Reservation> getCarReservations(String carId) {
        return reservations.stream()
                .filter(r -> r.getCarId().equals(carId))
                .collect(Collectors.toList());
    }

    /**
     * Checks how many cars of a given type are available for the specified dates.
     * 
     * @param type  the type of car
     * @param start the start time
     * @param days  the number of days
     * @return the number of available cars of the specified type
     */
    public int getAvailableCarsCount(CarType type, LocalDateTime start, int days) {
        return (int) cars.stream()
                .filter(car -> car.getType() == type)
                .filter(car -> isAvailable(car.getId(), start, days))
                .count();
    }

    /**
     * Checks if a specific car is available for the given dates.
     * 
     * @param carId the ID of the car
     * @param start the start time
     * @param days  the number of days
     * @return true if the car is available, false otherwise
     */
    private boolean isAvailable(String carId,
            LocalDateTime start,
            int days) {
        return reservations.stream()
                .filter(r -> r.getCarId().equals(carId))
                .noneMatch(r -> r.overlaps(start, days));
    }

    /**
     * Gets the total number of cars of a specific type.
     * 
     * @param type the type of car
     * @return the number of cars of that type
     */
    public int getTotalCarsCount(CarType type) {
        return (int) cars.stream()
                .filter(car -> car.getType() == type)
                .count();
    }

    /**
     * Gets all reservations in the system.
     * 
     * @return a list of all reservations
     */
    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations);
    }
}