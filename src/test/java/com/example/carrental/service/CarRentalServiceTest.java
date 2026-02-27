package com.example.carrental.service;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.exception.NoAvailableCarException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CarRentalService.
 * Tests core requirements (reservation, inventory limits, overlap detection),
 * edge cases, input validation, and additional features.
 */
class CarRentalServiceTest {

    private CarRentalService service;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        service = new CarRentalService(Map.of(
                CarType.SEDAN, 2,
                CarType.SUV, 1,
                CarType.VAN, 1));
        baseTime = LocalDateTime.of(2026, 3, 1, 10, 0);
    }

    // ========== Core Requirement Tests ==========

    @Test
    void shouldReserveCarWhenAvailable() {
        CarRentalService singleCarService = new CarRentalService(Map.of(CarType.SEDAN, 1));

        var reservation = singleCarService.reserveCar(
                CarType.SEDAN,
                LocalDateTime.now(),
                3);

        assertNotNull(reservation);
        assertEquals(CarType.SEDAN, reservation.getCarType());
    }

    @Test
    void shouldThrowExceptionWhenNoCarsAvailable() {
        CarRentalService singleCarService = new CarRentalService(Map.of(CarType.SEDAN, 1));

        LocalDateTime now = LocalDateTime.now();

        singleCarService.reserveCar(CarType.SEDAN, now, 3);

        assertThrows(NoAvailableCarException.class,
                () -> singleCarService.reserveCar(CarType.SEDAN, now, 3));
    }

    @Test
    void reserveDifferentTypes() {
        CarRentalService multiTypeService = new CarRentalService(Map.of(
                CarType.SEDAN, 1,
                CarType.SUV, 1,
                CarType.VAN, 1));

        assertNotNull(multiTypeService.reserveCar(CarType.SEDAN, LocalDateTime.now(), 1));
        assertNotNull(multiTypeService.reserveCar(CarType.SUV, LocalDateTime.now(), 1));
        assertNotNull(multiTypeService.reserveCar(CarType.VAN, LocalDateTime.now(), 1));
    }

    @Test
    void limitedInventoryEnforcesMaxCars() {
        CarRentalService twoCarService = new CarRentalService(Map.of(CarType.SEDAN, 2));

        LocalDateTime now = LocalDateTime.now();

        assertNotNull(twoCarService.reserveCar(CarType.SEDAN, now, 2));
        assertNotNull(twoCarService.reserveCar(CarType.SEDAN, now, 2));
        assertThrows(NoAvailableCarException.class,
                () -> twoCarService.reserveCar(CarType.SEDAN, now, 2));
    }

    @Test
    void overlappingReservationsAreRejected() {
        CarRentalService singleCarService = new CarRentalService(Map.of(CarType.SEDAN, 1));

        LocalDateTime start = LocalDateTime.of(2026, 2, 23, 10, 0);

        var r1 = singleCarService.reserveCar(CarType.SEDAN, start, 3);
        assertNotNull(r1);

        // overlaps with r1 - should throw exception
        assertThrows(NoAvailableCarException.class,
                () -> singleCarService.reserveCar(CarType.SEDAN, start.plusDays(1), 2));

        // starts after r1 ends - should succeed
        var r3 = singleCarService.reserveCar(CarType.SEDAN, start.plusDays(3), 1);
        assertNotNull(r3);
    }

    @Test
    void reservationEndIsCalculatedCorrectly() {
        CarRentalService suvService = new CarRentalService(Map.of(CarType.SUV, 1));

        LocalDateTime start = LocalDateTime.of(2026, 2, 23, 9, 0);
        var res = suvService.reserveCar(CarType.SUV, start, 5);

        assertEquals(start.plusDays(5), res.getEnd());
    }

    // ========== Input Validation Tests ==========

    @Test
    void throwExceptionWhenReservingWithZeroDays() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reserveCar(CarType.SEDAN, baseTime, 0),
                "Should throw for 0 days");
    }

    @Test
    void throwExceptionWhenReservingWithNegativeDays() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reserveCar(CarType.SEDAN, baseTime, -5),
                "Should throw for negative days");
    }

    @Test
    void throwExceptionWhenReservingWithNullCarType() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reserveCar(null, baseTime, 3),
                "Should throw for null car type");
    }

    @Test
    void throwExceptionWhenReservingWithNullStartDate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reserveCar(CarType.SEDAN, null, 3),
                "Should throw for null start date");
    }

    // ========== Overlap Detection Edge Cases ==========

    @Test
    void adjacentReservationsShouldBeAllowed() {
        var singleCarService = new CarRentalService(Map.of(CarType.SEDAN, 1));

        // First reservation: day 1-3
        Reservation first = singleCarService.reserveCar(CarType.SEDAN, baseTime, 3);
        assertEquals(baseTime.plusDays(3), first.getEnd());

        // Second reservation: day 3-5 (starts exactly when first ends)
        Reservation second = singleCarService.reserveCar(CarType.SEDAN, baseTime.plusDays(3), 2);
        assertNotNull(second);
        assertEquals(baseTime.plusDays(3), second.getStart());
    }

    @Test
    void partialOverlapShouldBeRejected() {
        var singleCarService = new CarRentalService(Map.of(CarType.SEDAN, 1));

        Reservation first = singleCarService.reserveCar(CarType.SEDAN, baseTime, 5);

        // Attempt to overlap by 1 day
        assertThrows(NoAvailableCarException.class,
                () -> singleCarService.reserveCar(CarType.SEDAN, baseTime.plusDays(4), 2));
    }

    @Test
    void completeOverlapShouldBeRejected() {
        var singleCarService = new CarRentalService(Map.of(CarType.SEDAN, 1));

        singleCarService.reserveCar(CarType.SEDAN, baseTime, 10);

        // Try to book the entire period again
        assertThrows(NoAvailableCarException.class,
                () -> singleCarService.reserveCar(CarType.SEDAN, baseTime, 10));
    }

    @Test
    void reservationBeforeShouldNotConflict() {
        var singleCarService = new CarRentalService(Map.of(CarType.SEDAN, 1));

        Reservation second = singleCarService.reserveCar(CarType.SEDAN, baseTime.plusDays(10), 3);
        assertNotNull(second);

        // Reservation ending exactly when the second starts should work
        Reservation first = singleCarService.reserveCar(CarType.SEDAN, baseTime, 10);
        assertNotNull(first);
    }

    // ========== Availability Query Tests ==========

    @Test
    void getAvailableCarsCountReturnsCorrectValue() {
        assertEquals(2, service.getAvailableCarsCount(CarType.SEDAN, baseTime, 3));
        assertEquals(1, service.getAvailableCarsCount(CarType.SUV, baseTime, 3));
        assertEquals(1, service.getAvailableCarsCount(CarType.VAN, baseTime, 3));
    }

    @Test
    void getAvailableCarsCountDecreasesAfterReservation() {
        service.reserveCar(CarType.SEDAN, baseTime, 3);
        assertEquals(1, service.getAvailableCarsCount(CarType.SEDAN, baseTime, 3));

        service.reserveCar(CarType.SEDAN, baseTime, 3);
        assertEquals(0, service.getAvailableCarsCount(CarType.SEDAN, baseTime, 3));
    }

    @Test
    void getAvailableCarsCountDoesNotAffectDifferentDateRanges() {
        service.reserveCar(CarType.SEDAN, baseTime, 3);

        // Different time period should still have 2 available
        assertEquals(2, service.getAvailableCarsCount(CarType.SEDAN, baseTime.plusDays(3), 3));
    }

    // ========== Cancellation Tests ==========

    @Test
    void cancelValidReservationReturnsTrue() {
        Reservation res = service.reserveCar(CarType.SEDAN, baseTime, 3);
        assertTrue(service.cancelReservation(res.getId()));
    }

    @Test
    void cancelInvalidReservationReturnsFalse() {
        assertFalse(service.cancelReservation("non-existent-id"));
    }

    @Test
    void afterCancellationCarBecomesAvailable() {
        var singleCarService = new CarRentalService(Map.of(CarType.SEDAN, 1));

        Reservation first = singleCarService.reserveCar(CarType.SEDAN, baseTime, 3);

        // Second reservation should fail (no cars available)
        assertThrows(NoAvailableCarException.class,
                () -> singleCarService.reserveCar(CarType.SEDAN, baseTime, 3));

        // Cancel first reservation
        assertTrue(singleCarService.cancelReservation(first.getId()));

        // Now second reservation should succeed
        Reservation second = singleCarService.reserveCar(CarType.SEDAN, baseTime, 3);
        assertNotNull(second);
    }

    // ========== Reservation Retrieval Tests ==========

    @Test
    void getCarReservationsReturnsAllReservationsForCar() {
        Reservation res1 = service.reserveCar(CarType.SEDAN, baseTime, 3);
        Reservation res2 = service.reserveCar(CarType.SEDAN, baseTime.plusDays(5), 2);

        var reservations = service.getCarReservations(res1.getCarId());
        assertEquals(2, reservations.size());
        assertTrue(reservations.stream().anyMatch(r -> r.getId().equals(res1.getId())));
        assertTrue(reservations.stream().anyMatch(r -> r.getId().equals(res2.getId())));
    }

    @Test
    void getCarReservationsForUnreservedCarReturnsEmpty() {
        service.reserveCar(CarType.SEDAN, baseTime, 3);
        var reservations = service.getCarReservations("unknown-car-id");
        assertEquals(0, reservations.size());
    }

    @Test
    void getAllReservationsReturnsAllReservations() {
        service.reserveCar(CarType.SEDAN, baseTime, 3);
        service.reserveCar(CarType.SUV, baseTime, 2);
        service.reserveCar(CarType.VAN, baseTime.plusDays(5), 1);

        assertEquals(3, service.getAllReservations().size());
    }

    // ========== Inventory Tests ==========

    @Test
    void getTotalCarsCountReturnsCorrectValue() {
        assertEquals(2, service.getTotalCarsCount(CarType.SEDAN));
        assertEquals(1, service.getTotalCarsCount(CarType.SUV));
        assertEquals(1, service.getTotalCarsCount(CarType.VAN));
    }

    @Test
    void getTotalCarsCountForNonExistentType() {
        // Create new service - default has none of this type
        var emptyService = new CarRentalService(Map.of(CarType.SEDAN, 1));
        assertEquals(0, emptyService.getTotalCarsCount(CarType.VAN));
    }

    // ========== Multi-Day Reservation Tests ==========

    @Test
    void longReservationWorks() {
        Reservation res = service.reserveCar(CarType.SEDAN, baseTime, 365);
        assertEquals(baseTime.plusDays(365), res.getEnd());
    }

    @Test
    void singleDayReservationWorks() {
        Reservation res = service.reserveCar(CarType.SEDAN, baseTime, 1);
        assertEquals(baseTime.plusDays(1), res.getEnd());
    }

    // ========== Backward Compatibility Tests ==========

    @Test
    void reserveCarOptionalReturnsEmptyWhenNoAvailable() {
        service.reserveCar(CarType.SEDAN, baseTime, 3);
        service.reserveCar(CarType.SEDAN, baseTime, 3);

        var result = service.reserveCarOptional(CarType.SEDAN, baseTime, 3);
        assertTrue(result.isEmpty());
    }

    @Test
    void reserveCarOptionalReturnsReservationWhenAvailable() {
        var result = service.reserveCarOptional(CarType.SEDAN, baseTime, 3);
        assertTrue(result.isPresent());
    }

    // ========== Reservation Properties Tests ==========

    @Test
    void reservationHasValidProperties() {
        Reservation res = service.reserveCar(CarType.SEDAN, baseTime, 5);

        assertNotNull(res.getId());
        assertNotNull(res.getCarId());
        assertEquals(CarType.SEDAN, res.getCarType());
        assertEquals(baseTime, res.getStart());
        assertEquals(5, res.getNumberOfDays());
        assertEquals(baseTime.plusDays(5), res.getEnd());
    }

    @Test
    void reservationIdIsUnique() {
        Reservation res1 = service.reserveCar(CarType.SEDAN, baseTime, 2);
        Reservation res2 = service.reserveCar(CarType.SEDAN, baseTime.plusDays(5), 2);

        assertNotEquals(res1.getId(), res2.getId());
    }

    @Test
    void reservationEqualsIsBasedOnId() {
        Reservation res1 = service.reserveCar(CarType.SEDAN, baseTime, 3);
        var allReservations = service.getAllReservations();
        Reservation res1Copy = allReservations.stream()
                .filter(r -> r.getId().equals(res1.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(res1, res1Copy);
    }
}