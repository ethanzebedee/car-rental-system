package com.example.carrental.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.exception.NoAvailableCarException;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.ReservationRepository;

import jakarta.annotation.PostConstruct;

/**
 * Service for managing car reservations in the rental system.
 * Handles reservation creation, cancellation, and availability queries.
 */
@Service
public class CarRentalService {

    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;

    public CarRentalService(CarRepository carRepository, ReservationRepository reservationRepository) {
        this.carRepository = carRepository;
        this.reservationRepository = reservationRepository;
    }

    @PostConstruct
    void seedInventory() {
        if (carRepository.count() == 0) {
            Map.of(CarType.SEDAN, 3, CarType.SUV, 2, CarType.VAN, 2).forEach((type, count) -> {
                for (int i = 0; i < count; i++) {
                    carRepository.save(new Car(type));
                }
            });
        }
    }

    /**
     * Reserves the first available car of the given type for the specified period.
     *
     * @throws IllegalArgumentException if any parameter is null or days &lt;= 0
     * @throws NoAvailableCarException  if no car of the requested type is free
     */
    @Transactional
    public Reservation reserveCar(CarType type, LocalDateTime start, int days) {
        if (type == null)
            throw new IllegalArgumentException("Car type cannot be null");
        if (start == null)
            throw new IllegalArgumentException("Start time cannot be null");
        if (days <= 0)
            throw new IllegalArgumentException("Number of days must be greater than 0");

        List<Car> carsOfType = carRepository.findByType(type);
        List<String> carIds = carsOfType.stream().map(Car::getId).toList();

        if (carIds.isEmpty()) {
            throw new NoAvailableCarException(
                    "No " + type + " cars available from " + start + " for " + days + " days");
        }
        Map<String, List<Reservation>> reservationsByCarId = reservationRepository
                .findByCarIdIn(carIds)
                .stream()
                .collect(Collectors.groupingBy(Reservation::getCarId));

        Car selected = carsOfType.stream()
                .filter(car -> {
                    List<Reservation> existing = reservationsByCarId.getOrDefault(car.getId(), List.of());
                    return existing.stream().noneMatch(r -> r.overlaps(start, days));
                })
                .findFirst()
                .orElseThrow(() -> new NoAvailableCarException(
                        "No " + type + " cars available from " + start + " for " + days + " days"));

        return reservationRepository.save(new Reservation(selected.getId(), type, start, days));
    }

    /**
     * Cancels a reservation by ID.
     *
     * @return true if the reservation existed and was cancelled, false otherwise
     */
    @Transactional
    public boolean cancelReservation(String reservationId) {
        if (reservationRepository.existsById(reservationId)) {
            reservationRepository.deleteById(reservationId);
            return true;
        }
        return false;
    }

    public List<Reservation> getCarReservations(String carId) {
        return reservationRepository.findByCarId(carId);
    }

    /**
     * Returns the number of available cars of the given type for the specified period.
     *
     * @throws IllegalArgumentException if any parameter is null or days &lt;= 0
     */
    public int getAvailableCarsCount(CarType type, LocalDateTime start, int days) {
        if (type == null)
            throw new IllegalArgumentException("Car type cannot be null");
        if (start == null)
            throw new IllegalArgumentException("Start time cannot be null");
        if (days <= 0)
            throw new IllegalArgumentException("Number of days must be greater than 0");

        List<Car> carsOfType = carRepository.findByType(type);
        List<String> carIds = carsOfType.stream().map(Car::getId).toList();

        Map<String, List<Reservation>> reservationsByCarId = reservationRepository
                .findByCarIdIn(carIds)
                .stream()
                .collect(Collectors.groupingBy(Reservation::getCarId));

        return (int) carsOfType.stream()
                .filter(car -> {
                    List<Reservation> existing = reservationsByCarId.getOrDefault(car.getId(), List.of());
                    return existing.stream().noneMatch(r -> r.overlaps(start, days));
                })
                .count();
    }

    public int getTotalCarsCount(CarType type) {
        return (int) carRepository.countByType(type);
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public Optional<Reservation> getReservationById(String id) {
        return reservationRepository.findById(id);
    }
}