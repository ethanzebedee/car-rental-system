package com.example.carrental.service;

import com.example.carrental.domain.CarType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CarRentalServiceTest {

    @Test
    void shouldReserveCarWhenAvailable() {
        CarRentalService service = new CarRentalService(Map.of(CarType.SEDAN, 1));

        var reservation = service.reserveCar(
                CarType.SEDAN,
                LocalDateTime.now(),
                3);

        assertTrue(reservation.isPresent());
    }

    @Test
    void shouldNotReserveWhenNoCarsAvailable() {
        CarRentalService service = new CarRentalService(Map.of(CarType.SEDAN, 1));

        LocalDateTime now = LocalDateTime.now();

        service.reserveCar(CarType.SEDAN, now, 3);

        var second = service.reserveCar(CarType.SEDAN, now, 3);

        assertTrue(second.isEmpty());
    }

    @Test
    void reserveDifferentTypes() {
        CarRentalService service = new CarRentalService(Map.of(
                CarType.SEDAN, 1,
                CarType.SUV, 1,
                CarType.VAN, 1));

        assertTrue(service.reserveCar(CarType.SEDAN, LocalDateTime.now(), 1).isPresent());
        assertTrue(service.reserveCar(CarType.SUV, LocalDateTime.now(), 1).isPresent());
        assertTrue(service.reserveCar(CarType.VAN, LocalDateTime.now(), 1).isPresent());
    }

    @Test
    void limitedInventoryAllowsMultipleReservationsUpToCount() {
        CarRentalService service = new CarRentalService(Map.of(CarType.SEDAN, 2));

        LocalDateTime now = LocalDateTime.now();

        assertTrue(service.reserveCar(CarType.SEDAN, now, 2).isPresent());
        assertTrue(service.reserveCar(CarType.SEDAN, now, 2).isPresent());
        assertTrue(service.reserveCar(CarType.SEDAN, now, 2).isEmpty());
    }

    @Test
    void overlappingReservationsAreRejected() {
        CarRentalService service = new CarRentalService(Map.of(CarType.SEDAN, 1));

        LocalDateTime start = LocalDateTime.of(2026, 2, 23, 10, 0);

        var r1 = service.reserveCar(CarType.SEDAN, start, 3);
        assertTrue(r1.isPresent());

        // overlaps with r1
        var r2 = service.reserveCar(CarType.SEDAN, start.plusDays(1), 2);
        assertTrue(r2.isEmpty());

        // starts after r1 ends
        var r3 = service.reserveCar(CarType.SEDAN, start.plusDays(3), 1);
        assertTrue(r3.isPresent());
    }

    @Test
    void reservationEndIsCalculatedCorrectly() {
        CarRentalService service = new CarRentalService(Map.of(CarType.SUV, 1));

        LocalDateTime start = LocalDateTime.of(2026, 2, 23, 9, 0);
        var res = service.reserveCar(CarType.SUV, start, 5).orElseThrow();

        assertEquals(start.plusDays(5), res.getEnd());
    }
}