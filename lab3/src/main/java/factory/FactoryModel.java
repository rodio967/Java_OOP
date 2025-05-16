package factory;

import factory.model.Car;
import factory.model.CarPart;
import factory.model.PartFactory;
import factory.model.parts.Accessory;
import factory.model.parts.Body;
import factory.model.parts.Engine;
import factory.storage.Storage;
import factory.threads.Dealer;
import factory.threads.StorageController;
import factory.threads.Supplier;

import java.util.*;

public class FactoryModel {
    public final Storage<Body> bodyStorage;
    public final Storage<Engine> engineStorage;
    public final Storage<Accessory> accessoryStorage;
    public final Storage<Car> carStorage;
    public final List<Dealer> dealers;
    public final StorageController controller;


    public class SupplierSettings<T extends CarPart> {
        public final Storage<T> storage;
        public final List<Supplier<T>> suppliers;
        public final PartFactory<T> factory;
        public final int delay;

        public SupplierSettings(Storage<T> storage, List<Supplier<T>> suppliers, PartFactory<T> factory, int delay) {
            this.storage = storage;
            this.suppliers = suppliers;
            this.factory = factory;
            this.delay = delay;
        }
    }

    public final Map<Class<? extends CarPart>, SupplierSettings<?>> supplierSettingsMap = new LinkedHashMap<>();

    private void initSupplierSettings(FactoryConfig config) {
        addSupplierSetting(Body.class, config.bodySuppliers, config.initialBodyDelay, config.bodyStorageCapacity, Body::new);
        addSupplierSetting(Engine.class, config.engineSuppliers, config.initialEngineDelay, config.engineStorageCapacity, Engine::new);
        addSupplierSetting(Accessory.class, config.accessorySuppliers, config.initialAccessoryDelay, config.accessoryStorageCapacity, Accessory::new);
    }

    private <T extends CarPart> void addSupplierSetting(Class<T> clazz, int count, int delay, int capacity,
                                                        PartFactory<T> factory) {
        Storage<T> storage = new Storage<>(capacity, clazz.getSimpleName());
        List<Supplier<T>> suppliers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Supplier<T> supplier = new Supplier<>(storage, factory, delay);
            suppliers.add(supplier);
            supplier.start();
        }


        supplierSettingsMap.put(clazz, new SupplierSettings<>(storage, suppliers, factory, delay));
    }

    @SuppressWarnings("unchecked")
    private <T extends CarPart> Storage<T> getStorageFromSettings(Class<T> clazz) {
        return (Storage<T>) supplierSettingsMap.get(clazz).storage;
    }

    public FactoryModel(FactoryConfig config) {
        initSupplierSettings(config);


        bodyStorage = getStorageFromSettings(Body.class);
        engineStorage = getStorageFromSettings(Engine.class);
        accessoryStorage = getStorageFromSettings(Accessory.class);

        carStorage = new Storage<>(config.carStorageCapacity, "Car");

        dealers = new ArrayList<>();
        for (int i = 0; i < config.dealers; i++) {
            Dealer dealer = new Dealer(carStorage, config.initialDealerDelay);
            dealers.add(dealer);
            dealer.start();
        }

        controller = new StorageController(carStorage, config, bodyStorage,
                engineStorage, accessoryStorage,
                config.targetStockLevel);
        controller.start();
    }

    public void shutdown() {
        supplierSettingsMap.values().forEach(setting -> setting.suppliers.forEach(Supplier::stopRunning));
        dealers.forEach(Dealer::stopRunning);
        controller.stopRunning();
    }

}
