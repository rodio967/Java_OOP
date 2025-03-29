package org.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;


import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseManager {
    private final String DB_PATH = "my-database/";
    //static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Table> tables = new HashMap<>();

    public String getDB_PATH() {return DB_PATH;}


    Gson customGson = new GsonBuilder()
            .registerTypeAdapter(Integer.class, new TypeAdapter<Integer>() {
                @Override
                public void write(JsonWriter out, Integer value) throws IOException {
                    out.value(value);
                }

                @Override
                public Integer read(JsonReader in) throws IOException {
                    return (int) in.nextDouble();
                }
            })
            .create();


    public void saveTable(String tableName, Table table) {
        try {
            Files.createDirectories(Paths.get(DB_PATH));
            String json = customGson.toJson(table);
            FileWriter writer = new FileWriter(DB_PATH + tableName + ".db");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении таблицы " + tableName + ": " + e.getMessage());
        }
    }

    public Table loadTable(String tableName) {
        try {
            String path = DB_PATH + tableName + ".db";
            if (!Files.exists(Paths.get(path))) {
                return null;
            }
            String json = new String(Files.readAllBytes(Paths.get(path)));
            Table table = customGson.fromJson(json, Table.class);


            for (Map<String, Object> row : table.getRows()) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getValue() instanceof Double) {
                        double value = (Double) entry.getValue();
                        if (value == (int) value) {
                            entry.setValue((int) value);
                        }
                    }
                }
            }


            return table;

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке таблицы " + tableName + ": " + e.getMessage());
            return null;
        }
    }

    public void loadAllTables() {
        try {
            Files.createDirectories(Paths.get(DB_PATH));
            Files.list(Paths.get(DB_PATH))
                    .filter(path -> path.toString().endsWith(".db"))
                    .forEach(path -> {
                        String tableName = path.getFileName().toString().replace(".db", "");
                        Table table = loadTable(tableName);
                        if (table != null) {
                            tables.put(tableName, table);
                            System.out.println("Загружена таблица: " + tableName);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке базы данных: " + e.getMessage());
        }
    }


    void createTable(String name, List<Column> columns) throws Exception {
        if (tables.containsKey(name)) {
            throw new Exception("Table " + name + " уже существует ");
        }

        Table table = new Table(name);
        for (Column col : columns) {
            table.addColumn(col.getName(), col.getType(), col.getisUnique(), col.getisNotNull());
        }
        tables.put(name, table);
    }

    void dropTable(String name) {
        Table table = tables.get(name);
        if (table == null) {
            throw new IllegalArgumentException("Ошибка: таблица " + name + " не найдена.");
        }
        tables.remove(name);
    }

    Table getTable(String name) {
        Table table = tables.get(name);
        if (table == null) {
            throw new IllegalArgumentException("Ошибка: таблица " + name + " не найдена.");
        }
        return table;
    }

    Set<String> listTables() {
        return tables.keySet();
    }
}