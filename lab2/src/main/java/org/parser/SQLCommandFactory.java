package org.parser;

import org.command.SQLCommand;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SQLCommandFactory {
    private static final Map<String, SQLCommand> commands = new HashMap<>();

    static {
        loadCommands();
    }

    private static void loadCommands() {
        Properties props = new Properties();
        try (InputStream input = SQLCommandFactory.class.getClassLoader().getResourceAsStream("sqlcommands.properties")) {
            if (input == null) {
                throw new RuntimeException("Файл sqlcommands.properties не найден!");
            }

            props.load(input);

            for (String key : props.stringPropertyNames()) {
                String className = props.getProperty(key);
                try {
                    Class<?> clazz = Class.forName(className);
                    SQLCommand command = (SQLCommand) clazz.getDeclaredConstructor().newInstance();
                    commands.put(key.toUpperCase(), command);
                } catch (Exception e) {
                    throw new RuntimeException("Не удалось загрузить команду: " + className, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения sqlcommands.properties", e);
        }
    }

    public static SQLCommand getCommand(String command) {
        if (!commands.containsKey(command)) {
            throw new IllegalArgumentException("Команда " + command + " не поддерживается");
        }
        return commands.get(command);
    }

}
