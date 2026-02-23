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
}