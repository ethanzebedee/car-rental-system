import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class CarRentalService {

    private final List<Car> cars = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();

    public CarRentalService(Map<CarType, Integer> initialInventory) {
        initialInventory.forEach((type, count) -> {
            for (int i = 0; i < count; i++) {
                cars.add(new Car(type));
            }
        });
    }

    public Optional<Reservation> reserveCar(CarType type,
            LocalDateTime start,
            int days) {

        List<Car> availableCars = cars.stream()
                .filter(car -> car.getType() == type)
                .filter(car -> isAvailable(car.getId(), start, days))
                .collect(Collectors.toList());

        if (availableCars.isEmpty()) {
            return Optional.empty();
        }

        Car selected = availableCars.get(0);
        Reservation reservation = new Reservation(selected.getId(), type, start, days);

        reservations.add(reservation);
        return Optional.of(reservation);
    }

    private boolean isAvailable(String carId,
            LocalDateTime start,
            int days) {
        return reservations.stream()
                .filter(r -> r.getCarId().equals(carId))
                .noneMatch(r -> r.overlaps(start, days));
    }
}