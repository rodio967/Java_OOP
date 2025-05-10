package org.database;

import com.google.gson.*;
import org.app.LoggerManager;
import org.model.Column;
import org.model.Table;

import java.util.logging.Logger;


import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseManager {
    private static final Logger logger = LoggerManager.getLogger(DatabaseManager.class);

    private final String DB_PATH = "my-database/";
    private final Map<String, Table> tables = new HashMap<>();

    public String getDB_PATH() {return DB_PATH;}


    private final Gson customGson = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {
                @Override
                public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                }
            })
            .registerTypeAdapter(ZonedDateTime.class, new JsonDeserializer<ZonedDateTime>() {
                @Override
                public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                        throws JsonParseException {
                    return ZonedDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            })
            .create();



    public void saveTable(String tableName, Table table) {
        try {
            Files.createDirectories(Paths.get(DB_PATH));

            String tableJson = customGson.toJson(table);

            FileWriter writer = new FileWriter(DB_PATH + tableName + ".db");
            writer.write(tableJson);

            writer.close();
        } catch (IOException e) {
            logger.severe("Ошибка при сохранении таблицы " + tableName + ": " + e.getMessage());
        }
    }


    public void ensure_DB_DirectoryExists() {
        try {
            Path dbPath = Paths.get(DB_PATH);
            if (Files.notExists(dbPath)) {
                Files.createDirectories(dbPath);
                logger.info("Создана директория базы данных: " + DB_PATH);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию базы данных: " + e.getMessage(), e);
        }
    }


    public Table loadTable(String tableName) {
        try {
            String path = DB_PATH + tableName + ".db";
            Path tablePath = Paths.get(path);

            if (!Files.exists(tablePath)) {
                return null;
            }

            String json = new String(Files.readAllBytes(tablePath));
            Table table = customGson.fromJson(json, Table.class);

            table.normalizeRowTypes();

            return table;

        } catch (IOException e) {
            logger.severe("Ошибка при загрузке таблицы " + tableName + ": " + e.getMessage());
            return null;
        }
    }

    public void loadAllTables() {
        try {
            Files.list(Paths.get(DB_PATH))
                    .filter(path -> path.toString().endsWith(".db"))
                    .forEach(path -> {
                        String tableName = path.getFileName().toString().replace(".db", "");
                        Table table = loadTable(tableName);
                        if (table != null) {
                            tables.put(tableName, table);
                            logger.info("Загружена таблица: " + tableName);
                        }
                    });
        } catch (IOException e) {
            logger.severe("Ошибка при загрузке базы данных: " + e.getMessage());
        }
    }


    public void createTable(String name, List<Column> columns) throws Exception {
        if (tables.containsKey(name)) {
            throw new Exception("Table " + name + " уже существует ");
        }

        Table table = new Table(name);
        for (Column col : columns) {
            table.addColumn(col.getName(), col.getType(), col.getIsUnique(), col.getIsNotNull());
        }
        tables.put(name, table);
    }

    public void dropTable(String name) {
        Table table = tables.get(name);
        if (table == null) {
            throw new IllegalArgumentException("Ошибка: таблица " + name + " не найдена.");
        }
        tables.remove(name);
    }

    public Table getTable(String name) {
        Table table = tables.get(name);
        if (table == null) {
            throw new IllegalArgumentException("Ошибка: таблица " + name + " не найдена.");
        }
        return table;
    }

    public Set<String> listTables() {
        return tables.keySet();
    }
}