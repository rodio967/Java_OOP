package factory.gui;

import factory.FactoryConfig;
import factory.model.PartFactory;
import factory.ThreadPool.FixedThreadPool;
import factory.ThreadPool.ThreadPool;
import factory.model.*;
import factory.model.parts.Accessory;
import factory.model.parts.Body;
import factory.model.parts.Engine;
import factory.storage.Storage;
import factory.threads.Dealer;
import factory.threads.StorageController;
import factory.threads.Supplier;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FactoryGUI extends JFrame {
    private final Storage<Body> bodyStorage;
    private final Storage<Engine> engineStorage;
    private final Storage<Accessory> accessoryStorage;
    private final Storage<Car> carStorage;
    private final List<Dealer> dealers;
    private final ThreadPool workers;
    private final StorageController controller;

    private JLabel bodyStorageLabel;
    private JLabel engineStorageLabel;
    private JLabel accessoryStorageLabel;
    private JLabel carStorageLabel;
    private JLabel bodyProducedLabel;
    private JLabel engineProducedLabel;
    private JLabel accessoryProducedLabel;
    private JLabel carsProducedLabel;

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

    private final Map<Class<? extends CarPart>, SupplierSettings<?>> supplierSettingsMap = new HashMap<>();

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





    public FactoryGUI(FactoryConfig config) {
        super("Car Factory Emulator");

        initSupplierSettings(config);


        bodyStorage = getStorageFromSettings(Body.class);
        engineStorage = getStorageFromSettings(Engine.class);
        accessoryStorage = getStorageFromSettings(Accessory.class);

        carStorage = new Storage<>(config.carStorageCapacity, "Car");

        workers = new FixedThreadPool(config.workers);

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


        setupGUI(config);

        Timer timer = new Timer(100, e -> updateInfo());
        timer.start();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    private void setupGUI(FactoryConfig config) {
        setLayout(new GridLayout(0, 2));

        JPanel infoPanel = createInfoPanel();
        JPanel controlPanel = createControlPanel(config);

        add(infoPanel);
        add(controlPanel);
    }

    private JPanel createDelaySlider(String title, int min, int max, int initial, Consumer<Integer> onChange) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(title), BorderLayout.NORTH);

        JSlider slider = new JSlider(min, max, initial);

        slider.setMajorTickSpacing((max - min) / 5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        JLabel valueLabel = new JLabel(String.valueOf(initial));
        slider.addChangeListener(e -> {
            int value = ((JSlider)e.getSource()).getValue();
            valueLabel.setText(String.valueOf(value));
            onChange.accept(value);
        });

        panel.add(slider, BorderLayout.CENTER);
        panel.add(valueLabel, BorderLayout.EAST);

        return panel;
    }


    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(0, 2));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Factory Status"));

        bodyStorageLabel = new JLabel("Body Storage: 0/" + bodyStorage.getCapacity());
        engineStorageLabel = new JLabel("Engine Storage: 0/" + engineStorage.getCapacity());
        accessoryStorageLabel = new JLabel("Accessory Storage: 0/" + accessoryStorage.getCapacity());
        carStorageLabel = new JLabel("Car Storage: 0/" + carStorage.getCapacity());
        bodyProducedLabel = new JLabel("Bodies Produced: 0");
        engineProducedLabel = new JLabel("Engines Produced: 0");
        accessoryProducedLabel = new JLabel("Accessories Produced: 0");
        carsProducedLabel = new JLabel("Cars Produced: 0");

        infoPanel.add(bodyStorageLabel);
        infoPanel.add(bodyProducedLabel);
        infoPanel.add(engineStorageLabel);
        infoPanel.add(engineProducedLabel);
        infoPanel.add(accessoryStorageLabel);
        infoPanel.add(accessoryProducedLabel);
        infoPanel.add(carStorageLabel);
        infoPanel.add(carsProducedLabel);

        return infoPanel;
    }

    private JPanel createControlPanel(FactoryConfig config) {
        JPanel controlPanel = new JPanel(new GridLayout(0, 1));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));


        supplierSettingsMap.forEach((clazz, setting) -> {
            controlPanel.add(createDelaySlider(clazz.getSimpleName() + " Supplier Delay", 100, 5000, setting.delay,
                    delay -> setting.suppliers.forEach(s -> s.setDelay(delay))
            ));
        });


        controlPanel.add(createDelaySlider("Dealer Delay", 100, 10000, config.initialDealerDelay,
                delay -> dealers.forEach(d -> d.setDelay(delay))));
        controlPanel.add(createDelaySlider("Target Stock Level", 1, carStorage.getCapacity(), config.targetStockLevel,
                controller::setTargetStock));

        return controlPanel;
    }

    private void updateInfo() {
        bodyStorageLabel.setText(String.format("Body Storage: %d/%d",
                bodyStorage.getSize(), bodyStorage.getCapacity()));
        engineStorageLabel.setText(String.format("Engine Storage: %d/%d",
                engineStorage.getSize(), engineStorage.getCapacity()));
        accessoryStorageLabel.setText(String.format("Accessory Storage: %d/%d",
                accessoryStorage.getSize(), accessoryStorage.getCapacity()));
        carStorageLabel.setText(String.format("Car Storage: %d/%d",
                carStorage.getSize(), carStorage.getCapacity()));

        bodyProducedLabel.setText(String.format("Bodies Produced: %d",
                bodyStorage.getProducedCount()));
        engineProducedLabel.setText(String.format("Engines Produced: %d",
                engineStorage.getProducedCount()));
        accessoryProducedLabel.setText(String.format("Accessories Produced: %d",
                accessoryStorage.getProducedCount()));
        carsProducedLabel.setText(String.format("Cars Produced: %d",
                carStorage.getProducedCount()));
    }

    public void shutdown() {
        supplierSettingsMap.values().forEach(setting -> setting.suppliers.forEach(Supplier::stopRunning));

        dealers.forEach(Dealer::stopRunning);
        controller.stopRunning();
        workers.shutdown();
    }
}
