package factory.threads;

import factory.model.Car;
import factory.storage.Storage;

public class Dealer extends Thread {
    private final Storage<Car> carStorage;
    private int delay;
    private volatile boolean running = true;

    public Dealer(Storage<Car> carStorage, int initialDelay) {
        this.carStorage = carStorage;
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
                Car car = carStorage.get();
                System.out.println("Дилер взял " + car);
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
