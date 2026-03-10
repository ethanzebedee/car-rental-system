package com.example.carrental.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.exception.NoAvailableCarException;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.ReservationRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

/**
 * Service for managing car reservations in the rental system
 * Handles reservation creation, cancellation, and availability queries
 * Thread-safe implementation using database transactions
 */
@Service
public class CarRentalService {

    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;

    // For testing purposes
    private final List<Car> cars;
    private final List<Reservation> reservations;

    @Autowired
    public CarRentalService(CarRepository carRepository, ReservationRepository reservationRepository) {
        this.carRepository = carRepository;
        this.reservationRepository = reservationRepository;
        this.cars = null;
        this.reservations = null;
    }

    @PostConstruct
    public void init() {
        initializeInventory(Map.of(
                CarType.SEDAN, 3,
                CarType.SUV, 2,
                CarType.VAN, 2));
    }

    // Constructor for testing
    public CarRentalService(Map<CarType, Integer> initialInventory) {
        this.carRepository = null;
        this.reservationRepository = null;
        this.cars = new CopyOnWriteArrayList<>();
        this.reservations = new CopyOnWriteArrayList<>();

        initialInventory.forEach((type, count) -> {
            for (int i = 0; i < count; i++) {
                cars.add(new Car(type));
            }
        });
    }

    private boolean isTestMode() {
        return carRepository == null;
    }

    /**
     * Initializes the car inventory with the given counts per type.
     * This should be called during application startup.
     */
    public void initializeInventory(Map<CarType, Integer> initialInventory) {
        // Clear existing data (for development/testing)
        carRepository.deleteAll();

        initialInventory.forEach((type, count) -> {
            for (int i = 0; i < count; i++) {
                carRepository.save(new Car(type));
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
    @Transactional
    public Reservation reserveCar(CarType type,
            LocalDateTime start,
            int days) {
        // validate inputs early to keep service usage safe
        if (type == null) {
            throw new IllegalArgumentException("Car type cannot be null");
        }
        if (start == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Number of days must be greater than 0");
        }

        if (isTestMode()) {
            // Test mode: use in-memory lists
            Car selectedCar = cars.stream()
                    .filter(car -> car.getType() == type)
                    .filter(car -> isAvailable(car.getId(), start, days))
                    .findFirst()
                    .orElseThrow(() -> new NoAvailableCarException(
                            "No " + type + " cars available from " + start + " for " + days + " days"));

            Reservation reservation = new Reservation(selectedCar.getId(), type, start, days);
            reservations.add(reservation);
            return reservation;
        } else {
            // Production mode: use repositories
            Car selectedCar = carRepository.findByType(type).stream()
                    .filter(car -> isAvailable(car.getId(), start, days))
                    .findFirst()
                    .orElseThrow(() -> new NoAvailableCarException(
                            "No " + type + " cars available from " + start + " for " + days + " days"));

            Reservation reservation = new Reservation(selectedCar.getId(), type, start, days);
            return reservationRepository.save(reservation);
        }
    }

    /**
     * Reserves a car
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
    @Transactional
    public boolean cancelReservation(String reservationId) {
        if (isTestMode()) {
            return reservations.removeIf(r -> r.getId().equals(reservationId));
        } else {
            if (reservationRepository.existsById(reservationId)) {
                reservationRepository.deleteById(reservationId);
                return true;
            }
            return false;
        }
    }

    /**
     * Retrieves all reservations for a specific car.
     * 
     * @param carId the ID of the car
     * @return a list of all reservations for the car
     */
    public List<Reservation> getCarReservations(String carId) {
        if (isTestMode()) {
            return reservations.stream()
                    .filter(r -> r.getCarId().equals(carId))
                    .collect(Collectors.toList());
        } else {
            return reservationRepository.findByCarId(carId);
        }
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
        if (isTestMode()) {
            return (int) cars.stream()
                    .filter(car -> car.getType() == type)
                    .filter(car -> isAvailable(car.getId(), start, days))
                    .count();
        } else {
            return (int) carRepository.findByType(type).stream()
                    .filter(car -> isAvailable(car.getId(), start, days))
                    .count();
        }
    }

    /**
     * Helper used during reservation logic. We look only at existing
     * reservations for *this* car and ensure none of them overlap the
     * requested window.
     */
    private boolean isAvailable(String carId,
            LocalDateTime start,
            int days) {
        if (isTestMode()) {
            return reservations.stream()
                    .filter(r -> r.getCarId().equals(carId))
                    .noneMatch(r -> r.overlaps(start, days));
        } else {
            return reservationRepository.findByCarId(carId).stream()
                    .noneMatch(r -> r.overlaps(start, days));
        }
    }

    /**
     * Gets the total number of cars of a specific type.
     * 
     * @param type the type of car
     * @return the number of cars of that type
     */
    public int getTotalCarsCount(CarType type) {
        if (isTestMode()) {
            return (int) cars.stream()
                    .filter(car -> car.getType() == type)
                    .count();
        } else {
            return (int) carRepository.countByType(type);
        }
    }

    /**
     * Gets all reservations in the system.
     * 
     * @return a list of all reservations
     */
    public List<Reservation> getAllReservations() {
        if (isTestMode()) {
            return new ArrayList<>(reservations);
        } else {
            return reservationRepository.findAll();
        }
    }
}