package chat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;



public class Config {
    private static final Properties props = new Properties();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Config file not found in classpath");
            }
            props.load(input);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file", e);
        }
    }

    public static String getServerIp() {
        return props.getProperty("server.ip");
    }

    public static int getServerPort() {
        return Integer.parseInt(props.getProperty("server.port"));
    }

    public static ProtocolType getProtocolType() {
        return ProtocolType.valueOf(props.getProperty("protocol.type"));
    }

    public enum ProtocolType {
        XML, OBJECT
    }
}
