import java.time.LocalDateTime;

public class Reservation {
    private final String carId;
    private final CarType carType;
    private final LocalDateTime start;
    private final int numberOfDays;

    public Reservation(String carId, CarType carType,
            LocalDateTime start, int numberOfDays) {
        this.carId = carId;
        this.carType = carType;
        this.start = start;
        this.numberOfDays = numberOfDays;
    }

    public LocalDateTime getEnd() {
        return start.plusDays(numberOfDays);
    }

    public boolean overlaps(LocalDateTime otherStart, int otherDays) {
        LocalDateTime otherEnd = otherStart.plusDays(otherDays);
        return start.isBefore(otherEnd) && getEnd().isAfter(otherStart);
    }

    public String getCarId() {
        return carId;
    }
}