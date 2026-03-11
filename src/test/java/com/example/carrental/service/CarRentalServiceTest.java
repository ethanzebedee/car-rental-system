package com.example.carrental.service;

import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.exception.NoAvailableCarException;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CarRentalService using Mockito to mock repository
 * dependencies.
 * Tests run against the real production code path rather than an in-memory
 * stub.
 */
@ExtendWith(MockitoExtension.class)
class CarRentalServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private CarRentalService service;

    private LocalDateTime baseTime;
    private Car sedan1, sedan2, suv1, van1;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2026, 3, 1, 10, 0);
        sedan1 = new Car(CarType.SEDAN);
        sedan2 = new Car(CarType.SEDAN);
        suv1 = new Car(CarType.SUV);
        van1 = new Car(CarType.VAN);
    }

    // ========== Required Tests ==========

    @Test
    void shouldReserveCarWhenAvailable() {
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Reservation reservation = service.reserveCar(CarType.SEDAN, baseTime, 3);

        assertNotNull(reservation);
        assertEquals(CarType.SEDAN, reservation.getCarType());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void shouldThrowExceptionWhenNoCarsAvailable() {
        Reservation blocking = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime, 3);
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of(blocking));

        assertThrows(NoAvailableCarException.class,
                () -> service.reserveCar(CarType.SEDAN, baseTime, 3));
    }

    @Test
    void reserveDifferentTypes() {
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(carRepository.findByTypeWithLock(CarType.SUV)).thenReturn(List.of(suv1));
        when(carRepository.findByTypeWithLock(CarType.VAN)).thenReturn(List.of(van1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.reserveCar(CarType.SEDAN, baseTime, 1));
        assertNotNull(service.reserveCar(CarType.SUV, baseTime, 1));
        assertNotNull(service.reserveCar(CarType.VAN, baseTime, 1));
    }

    @Test
    void limitedInventoryEnforcesMaxCars() {
        Reservation r1 = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime, 2);
        Reservation r2 = new Reservation(sedan2.getId(), CarType.SEDAN, baseTime, 2);
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1, sedan2));
        when(reservationRepository.findByCarIdIn(anyList()))
                .thenReturn(List.of()) // first booking: no conflicts
                .thenReturn(List.of()) // second booking: no conflicts
                .thenReturn(List.of(r1, r2)); // third booking: both cars taken
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.reserveCar(CarType.SEDAN, baseTime, 2));
        assertNotNull(service.reserveCar(CarType.SEDAN, baseTime, 2));
        assertThrows(NoAvailableCarException.class,
                () -> service.reserveCar(CarType.SEDAN, baseTime, 2));
    }

    @Test
    void overlappingReservationsAreRejected() {
        LocalDateTime start = LocalDateTime.of(2026, 2, 23, 10, 0);
        Reservation blocking = new Reservation(sedan1.getId(), CarType.SEDAN, start, 3);
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of(blocking));

        // starts within the existing reservation — should be rejected
        assertThrows(NoAvailableCarException.class,
                () -> service.reserveCar(CarType.SEDAN, start.plusDays(1), 2));
    }

    @Test
    void adjacentReservationIsAllowed() {
        LocalDateTime start = LocalDateTime.of(2026, 2, 23, 10, 0);
        Reservation existing = new Reservation(sedan1.getId(), CarType.SEDAN, start, 3);
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of(existing));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // starts exactly when existing ends — should be allowed
        Reservation adjacent = service.reserveCar(CarType.SEDAN, start.plusDays(3), 2);
        assertNotNull(adjacent);
        assertEquals(start.plusDays(3), adjacent.getStart());
    }

    @Test
    void reservationEndIsCalculatedCorrectly() {
        when(carRepository.findByTypeWithLock(CarType.SUV)).thenReturn(List.of(suv1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LocalDateTime start = LocalDateTime.of(2026, 2, 23, 9, 0);
        Reservation res = service.reserveCar(CarType.SUV, start, 5);

        assertEquals(start.plusDays(5), res.getEnd());
    }

    // ========== Input Validation Tests ==========

    @Test
    void throwExceptionWhenReservingWithZeroDays() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reserveCar(CarType.SEDAN, baseTime, 0));
    }

    @Test
    void throwExceptionWhenReservingWithNegativeDays() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reserveCar(CarType.SEDAN, baseTime, -5));
    }

    @Test
    void throwExceptionWhenReservingWithNullCarType() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reserveCar(null, baseTime, 3));
    }

    @Test
    void throwExceptionWhenReservingWithNullStartDate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reserveCar(CarType.SEDAN, null, 3));
    }

    // ========== Overlap Detection Edge Cases ==========

    @Test
    void partialOverlapShouldBeRejected() {
        Reservation existing = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime, 5);
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of(existing));

        assertThrows(NoAvailableCarException.class,
                () -> service.reserveCar(CarType.SEDAN, baseTime.plusDays(4), 2));
    }

    @Test
    void completeOverlapShouldBeRejected() {
        Reservation existing = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime, 10);
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of(existing));

        assertThrows(NoAvailableCarException.class,
                () -> service.reserveCar(CarType.SEDAN, baseTime, 10));
    }

    @Test
    void reservationEndingAtStartOfExistingIsAllowed() {
        // Existing starts at +10; new reservation ends exactly at +10
        Reservation existing = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime.plusDays(10), 3);
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of(existing));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Reservation before = service.reserveCar(CarType.SEDAN, baseTime, 10);
        assertNotNull(before);
    }

    // ========== Availability Query Tests ==========

    @Test
    void getAvailableCarsCountReturnsCorrectValue() {
        when(carRepository.findByType(CarType.SEDAN)).thenReturn(List.of(sedan1, sedan2));
        when(carRepository.findByType(CarType.SUV)).thenReturn(List.of(suv1));
        when(carRepository.findByType(CarType.VAN)).thenReturn(List.of(van1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of());

        assertEquals(2, service.getAvailableCarsCount(CarType.SEDAN, baseTime, 3));
        assertEquals(1, service.getAvailableCarsCount(CarType.SUV, baseTime, 3));
        assertEquals(1, service.getAvailableCarsCount(CarType.VAN, baseTime, 3));
    }

    @Test
    void getAvailableCarsCountDecreasesWithExistingReservation() {
        Reservation r1 = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime, 3);
        when(carRepository.findByType(CarType.SEDAN)).thenReturn(List.of(sedan1, sedan2));
        // sedan1 is blocked; sedan2 is free
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of(r1));

        assertEquals(1, service.getAvailableCarsCount(CarType.SEDAN, baseTime, 3));
    }

    @Test
    void getAvailableCarsCountDoesNotAffectDifferentDateRanges() {
        Reservation r1 = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime, 3);
        when(carRepository.findByType(CarType.SEDAN)).thenReturn(List.of(sedan1, sedan2));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of(r1));

        // An existing baseTime reservation doesn't block the baseTime+3 period
        assertEquals(2, service.getAvailableCarsCount(CarType.SEDAN, baseTime.plusDays(3), 3));
    }

    // ========== getAvailableCarsCount Input Validation Tests ==========

    @Test
    void getAvailableCarsCountThrowsOnNullType() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAvailableCarsCount(null, baseTime, 3));
    }

    @Test
    void getAvailableCarsCountThrowsOnNullStart() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAvailableCarsCount(CarType.SEDAN, null, 3));
    }

    @Test
    void getAvailableCarsCountThrowsOnZeroDays() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAvailableCarsCount(CarType.SEDAN, baseTime, 0));
    }

    @Test
    void getAvailableCarsCountThrowsOnNegativeDays() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAvailableCarsCount(CarType.SEDAN, baseTime, -1));
    }

    // ========== Cancellation Tests ==========

    @Test
    void cancelValidReservationReturnsTrue() {
        when(reservationRepository.deleteReservationById("res-1")).thenReturn(1);

        assertTrue(service.cancelReservation("res-1"));
        verify(reservationRepository).deleteReservationById("res-1");
    }

    @Test
    void cancelInvalidReservationReturnsFalse() {
        when(reservationRepository.deleteReservationById("no-such-id")).thenReturn(0);

        assertFalse(service.cancelReservation("no-such-id"));
        verify(reservationRepository).deleteReservationById("no-such-id");
    }

    // ========== Retrieval Tests ==========

    @Test
    void getCarReservationsReturnsAllReservationsForCar() {
        Reservation r1 = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime, 3);
        Reservation r2 = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime.plusDays(5), 2);
        when(reservationRepository.findByCarId(sedan1.getId())).thenReturn(List.of(r1, r2));

        assertEquals(2, service.getCarReservations(sedan1.getId()).size());
    }

    @Test
    void getCarReservationsForUnreservedCarReturnsEmpty() {
        when(reservationRepository.findByCarId("unknown-car-id")).thenReturn(List.of());

        assertEquals(0, service.getCarReservations("unknown-car-id").size());
    }

    @Test
    void getAllReservationsReturnsAllReservations() {
        Reservation r1 = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime, 3);
        Reservation r2 = new Reservation(suv1.getId(), CarType.SUV, baseTime, 2);
        when(reservationRepository.findAll()).thenReturn(List.of(r1, r2));

        assertEquals(2, service.getAllReservations().size());
    }

    @Test
    void getReservationByIdReturnsReservationWhenFound() {
        Reservation res = new Reservation(sedan1.getId(), CarType.SEDAN, baseTime, 3);
        when(reservationRepository.findById(res.getId())).thenReturn(Optional.of(res));

        Optional<Reservation> result = service.getReservationById(res.getId());
        assertTrue(result.isPresent());
        assertEquals(res.getId(), result.get().getId());
    }

    @Test
    void getReservationByIdReturnsEmptyWhenNotFound() {
        when(reservationRepository.findById("missing")).thenReturn(Optional.empty());

        assertTrue(service.getReservationById("missing").isEmpty());
    }

    // ========== Inventory Tests ==========

    @Test
    void getTotalCarsCountReturnsCorrectValue() {
        when(carRepository.countByType(CarType.SEDAN)).thenReturn(2L);
        when(carRepository.countByType(CarType.SUV)).thenReturn(1L);

        assertEquals(2, service.getTotalCarsCount(CarType.SEDAN));
        assertEquals(1, service.getTotalCarsCount(CarType.SUV));
    }

    // ========== Reservation Properties Tests ==========

    @Test
    void reservationHasValidProperties() {
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

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
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1, sedan2));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Reservation res1 = service.reserveCar(CarType.SEDAN, baseTime, 2);
        Reservation res2 = service.reserveCar(CarType.SEDAN, baseTime, 2);

        assertNotEquals(res1.getId(), res2.getId());
        // Verify different cars were selected, confirming conflict detection ran
        assertNotEquals(res1.getCarId(), res2.getCarId());
    }

    // ========== Multi-Day Reservation Tests ==========

    @Test
    void longReservationWorks() {
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Reservation res = service.reserveCar(CarType.SEDAN, baseTime, 365);
        assertEquals(baseTime.plusDays(365), res.getEnd());
    }

    @Test
    void singleDayReservationWorks() {
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Reservation res = service.reserveCar(CarType.SEDAN, baseTime, 1);
        assertEquals(baseTime.plusDays(1), res.getEnd());
    }

    // ========== Concurrency Tests ==========

    /**
     * Verifies that {@code reserveCar} always invokes {@code findByTypeWithLock}
     * (rather than the unlocked {@code findByType}) so that the pessimistic-lock
     * path is exercised on every booking attempt.
     */
    @Test
    void reserveCarUsesPessimisticLockQuery() {
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.reserveCar(CarType.SEDAN, baseTime, 3);

        verify(carRepository).findByTypeWithLock(CarType.SEDAN);
        verify(carRepository, never()).findByType(CarType.SEDAN);
    }

    /**
     * Simulates concurrent reservation attempts and verifies that the service
     * does not throw unexpected exceptions and that every call either
     * succeeds or raises {@link NoAvailableCarException}.
     *
     * <p>Note: unit-test mocks cannot reproduce database-level pessimistic
     * locking; the exact number of successes is non-deterministic here.
     * The {@link #reserveCarUsesPessimisticLockQuery()} test verifies that
     * the locking query is always invoked, while integration tests against a
     * real DB would confirm exactly-once booking semantics.</p>
     */
    @Test
    void concurrentReservationsDoNotCorrupt() throws InterruptedException {
        // Arrange: single sedan available, shared mutable reservation list
        List<Reservation> saved = new ArrayList<>();
        when(carRepository.findByTypeWithLock(CarType.SEDAN)).thenReturn(List.of(sedan1));
        when(reservationRepository.findByCarIdIn(anyList())).thenAnswer(inv -> List.copyOf(saved));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            synchronized (saved) {
                saved.add(r);
            }
            return r;
        });

        int threads = 5;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    service.reserveCar(CarType.SEDAN, baseTime, 3);
                    successes.incrementAndGet();
                } catch (NoAvailableCarException e) {
                    failures.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (java.util.concurrent.ExecutionException e) {
                // Re-throw any unexpected exception so the test fails visibly
                throw new RuntimeException("Unexpected exception in concurrent thread", e.getCause());
            }
        }

        // Every call must either succeed or raise NoAvailableCarException
        assertEquals(threads, successes.get() + failures.get(),
                "Every thread must have either succeeded or received NoAvailableCarException");
        assertTrue(successes.get() >= 1, "At least one reservation must succeed");
    }
}
