package factory.model;

import factory.model.parts.Accessory;
import factory.model.parts.Body;
import factory.model.parts.Engine;

import java.util.concurrent.atomic.AtomicInteger;

public class Car {
    private static AtomicInteger nextId = new AtomicInteger(1);
    private final int id;
    private final Body body;
    private final Engine engine;
    private final Accessory accessory;

    public Car(Body body, Engine engine, Accessory accessory) {
        this.id = nextId.getAndIncrement();
        this.body = body;
        this.engine = engine;
        this.accessory = accessory;
    }

    public int getId() { return id; }

    @Override
    public String toString() {
        return "Car #" + id + " [Body: " + body.getId() +
                ", Engine: " + engine.getId() +
                ", Accessory: " + accessory.getId() + "]";
    }
}
