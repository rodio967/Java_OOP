package calculator;

import calculator.commands.Command;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CommandFactory {
    private static final Map<String, Command> commands = new HashMap<>();

    static {
        loadCommands();
    }

    private static void loadCommands() {
        Properties properties = new Properties();
        try (InputStream input = CommandFactory.class.getClassLoader().getResourceAsStream("commands.properties")) {
            if (input == null) {
                throw new RuntimeException("Файл commands.properties не найден!");
            }
            properties.load(input);

            for (String key : properties.stringPropertyNames()) {
                String className = properties.getProperty(key);
                try {
                    Class<?> clazz = Class.forName(className);
                    Command command = (Command) clazz.getDeclaredConstructor().newInstance();
                    commands.put(key, command);
                } catch (Exception e) {
                    throw new RuntimeException("Ошибка загрузки команды: " + className, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки файла commands.properties", e);
        }
    }

    public static Command getCommand(String name) {
        return commands.getOrDefault(name, (ctx, args) -> {});
    }
}
