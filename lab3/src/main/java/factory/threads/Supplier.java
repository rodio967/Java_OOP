package factory.threads;

import factory.model.CarPart;
import factory.model.PartFactory;
import factory.storage.Storage;

public class Supplier<T extends CarPart> extends Thread {
    private final Storage<T> storage;
    private final PartFactory<T> factory;
    private int delay;
    private volatile boolean running = true;

    public Supplier(Storage<T> storage, PartFactory<T> factory, int initialDelay) {
        this.storage = storage;
        this.factory = factory;
        this.delay = initialDelay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void stopRunning() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        try {
            while (running) {
                T part = createPart();
                storage.add(part);
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    private T createPart() {
        return factory.create();
    }
}
