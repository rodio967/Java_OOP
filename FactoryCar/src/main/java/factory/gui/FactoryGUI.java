package factory.gui;

import factory.FactoryConfig;
import factory.FactoryModel;
import factory.model.*;
import factory.model.parts.Accessory;
import factory.model.parts.Body;
import factory.model.parts.Engine;
import factory.storage.Storage;
import factory.threads.Dealer;
import factory.threads.StorageController;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class FactoryGUI extends JFrame {
    private final Storage<Body> bodyStorage;
    private final Storage<Engine> engineStorage;
    private final Storage<Accessory> accessoryStorage;
    private final Storage<Car> carStorage;
    private final List<Dealer> dealers;
    private final StorageController controller;
    private final FactoryModel model;

    private JLabel bodyStorageLabel;
    private JLabel engineStorageLabel;
    private JLabel accessoryStorageLabel;
    private JLabel carStorageLabel;
    private JLabel bodyProducedLabel;
    private JLabel engineProducedLabel;
    private JLabel accessoryProducedLabel;
    private JLabel carsProducedLabel;


    public FactoryGUI(FactoryConfig config, FactoryModel model) {
        super("Car Factory Emulator");

        this.model = model;

        bodyStorage = model.bodyStorage;
        engineStorage = model.engineStorage;
        accessoryStorage = model.accessoryStorage;
        carStorage = model.carStorage;

        dealers = model.dealers;
        controller = model.controller;


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


        model.supplierSettingsMap.forEach((clazz, setting) -> {
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
        model.shutdown();
    }
}
