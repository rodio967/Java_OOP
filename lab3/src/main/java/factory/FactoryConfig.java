package factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FactoryConfig {
    public int bodyStorageCapacity;
    public int engineStorageCapacity;
    public int accessoryStorageCapacity;
    public int carStorageCapacity;
    public int bodySuppliers;
    public int engineSuppliers;
    public int accessorySuppliers;
    public int workers;
    public int dealers;
    public int initialBodyDelay;
    public int initialEngineDelay;
    public int initialAccessoryDelay;
    public int initialDealerDelay;
    public int targetStockLevel;

    public static FactoryConfig loadFromFile(String filename) throws IOException {
        FactoryConfig config = new FactoryConfig();
        Properties props = new Properties();
        try (InputStream in = FactoryConfig.class.getClassLoader().getResourceAsStream(filename)) {
            props.load(in);
        }

        config.bodyStorageCapacity = Integer.parseInt(props.getProperty("bodyStorageCapacity", "10"));
        config.engineStorageCapacity = Integer.parseInt(props.getProperty("engineStorageCapacity", "10"));
        config.accessoryStorageCapacity = Integer.parseInt(props.getProperty("accessoryStorageCapacity", "10"));
        config.carStorageCapacity = Integer.parseInt(props.getProperty("carStorageCapacity", "10"));
        config.bodySuppliers = Integer.parseInt(props.getProperty("bodySuppliers", "1"));
        config.engineSuppliers = Integer.parseInt(props.getProperty("engineSuppliers", "1"));
        config.accessorySuppliers = Integer.parseInt(props.getProperty("accessorySuppliers", "1"));
        config.workers = Integer.parseInt(props.getProperty("workers", "2"));
        config.dealers = Integer.parseInt(props.getProperty("dealers", "2"));
        config.initialBodyDelay = Integer.parseInt(props.getProperty("initialBodyDelay", "1000"));
        config.initialEngineDelay = Integer.parseInt(props.getProperty("initialEngineDelay", "1000"));
        config.initialAccessoryDelay = Integer.parseInt(props.getProperty("initialAccessoryDelay", "1000"));
        config.initialDealerDelay = Integer.parseInt(props.getProperty("initialDealerDelay", "2000"));
        config.targetStockLevel = Integer.parseInt(props.getProperty("targetStockLevel", "5"));

        return config;
    }
}
