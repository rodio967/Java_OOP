package factory.threads;

import factory.*;
import factory.ThreadPool.FixedThreadPool;
import factory.ThreadPool.ThreadPool;
import factory.model.parts.Accessory;
import factory.model.parts.Body;
import factory.model.Car;
import factory.model.parts.Engine;
import factory.storage.Storage;

import java.util.concurrent.atomic.AtomicInteger;

public class StorageController extends Thread {
    private final Storage<Car> carStorage;
    private final ThreadPool workers;
    private final Storage<Body> bodyStorage;
    private final Storage<Engine> engineStorage;
    private final Storage<Accessory> accessoryStorage;
    private volatile boolean running = true;
    private int targetStock;
    private final AtomicInteger pendingCars = new AtomicInteger(0);

    public StorageController(Storage<Car> carStorage, FactoryConfig config, Storage<Body> bodyStorage, Storage<Engine> engineStorage,
                             Storage<Accessory> accessoryStorage, int targetStock) {
        this.carStorage = carStorage;
        this.workers = new FixedThreadPool(config.workers);
        this.bodyStorage = bodyStorage;
        this.engineStorage = engineStorage;
        this.accessoryStorage = accessoryStorage;
        this.targetStock = targetStock;
    }

    public void setTargetStock(int targetStock) {
        this.targetStock = targetStock;
    }

    public void stopRunning() {
        running = false;
        interrupt();
    }


    @Override
    public void run() {
        try {
            while (running) {
                synchronized (carStorage) {
                    while (carStorage.getSize() >= targetStock) {
                        carStorage.wait();
                    }
                }


                int needed = targetStock - (carStorage.getSize() + pendingCars.get());

                if (needed > 0) {
                    pendingCars.addAndGet(needed);

                    for (int i = 0; i < needed; i++) {
                        workers.execute(() -> {
                            assembleCar();
                            pendingCars.decrementAndGet();
                        });
                    }
                }

                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void assembleCar() {
        try {
            Body body = bodyStorage.get();
            Engine engine = engineStorage.get();
            Accessory accessory = accessoryStorage.get();

            Car car = new Car(body, engine, accessory);
            carStorage.add(car);
            System.out.println("Изготовлена " + car);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
