package factory.model;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class CarPart {
    private static AtomicInteger nextId = new AtomicInteger(1);
    private final int id;
    private final String type;

    public CarPart(String type) {
        this.id = nextId.getAndIncrement();
        this.type = type;
    }

    public int getId() { return id; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return type + " #" + id;
    }
}
