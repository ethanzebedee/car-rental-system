import java.util.UUID;

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