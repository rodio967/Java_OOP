package org.model;



import org.app.LoggerManager;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;


public class Table {
    private static final Logger logger = LoggerManager.getLogger(Table.class);
    private final String name;
    private final LinkedHashMap<String, Column> columns = new LinkedHashMap<>();
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private final Map<String, Set<Object>> uniqueValues = new HashMap<>();

    public Table(String name) {
        this.name = name;
    }

    public String getName() {return name;}

    public LinkedHashMap<String, Column> getColumns() {return columns;}

    public List<Map<String, Object>> getRows() {return rows;}

    public void addNewColumn(String columnName, String type, boolean isUnique, boolean isNotNull) {
        addColumn(columnName, type, isUnique, isNotNull);

        for (Map<String, Object> row : rows) {
            row.put(columnName, null);
        }
    }


    public void addColumn(String name, String type, boolean isUnique, boolean isNotNull) {
        if (columns.containsKey(name)) {
            throw new IllegalArgumentException("Столбец уже существует: " + name);
        }

        Column column = new Column(name, type, isUnique, isNotNull);
        columns.put(name, column);

        if (isUnique) {
            uniqueValues.put(name, new HashSet<>());
        }
    }


    public List<String> getColumnNames() {
        return new ArrayList<>(columns.keySet());
    }

    public void dropColumn(String columnName) {
        if (!columns.containsKey(columnName)) {
            throw new IllegalArgumentException("Столбец '" + columnName + "' не найден.");
        }

        uniqueValues.remove(columnName);

        columns.remove(columnName);
        for (Map<String, Object> row : rows) {
            row.remove(columnName);
        }
    }



    public Column getColumn(String name) {
        Column column = columns.get(name);
        if (column == null) {
            throw new IllegalArgumentException("Ошибка: Столбец " + name + " не найден.");
        }
        return column;
    }


    public void insertRow(Map<String, Object> row) throws Exception {
        for (Column column : columns.values()) {
            String columnName = column.getName();
            Object valueRow = row.get(columnName);

            if (column.getIsNotNull()) {
                if (valueRow instanceof String) {
                    String value = (String) valueRow;
                    if (value.isEmpty()) {
                        throw new IllegalArgumentException("Ошибка: Поле '" + column.getName() + "' не может быть NULL.");
                    }
                }
            }


            if (column.getIsUnique()) {
                Set<Object> existingValues = uniqueValues.get(columnName);

                if (existingValues.contains(valueRow)) {
                    throw new Exception("Ошибка: Столбец '" + columnName + "' должен быть уникальным.");
                }
            }
        }
        rows.add(row);

        for (String columnName : uniqueValues.keySet()) {
            Object value = row.get(columnName);
            if (!Objects.equals(value, "")) {
                uniqueValues.get(columnName).add(value);
            }
        }

    }

    public void deleteRows(Map<String, Object> conditions) {
        Iterator<Map<String, Object>> iterator = rows.iterator();

        while (iterator.hasNext()) {
            Map<String, Object> row = iterator.next();
            if (matchesConditions(row, conditions)) {
                for (String columnName : uniqueValues.keySet()) {
                    Object value = row.get(columnName);
                    if (!Objects.equals(value, "")) {
                        uniqueValues.get(columnName).remove(value);
                    }
                }

                iterator.remove();
                logger.info("Удалена строка из таблицы " + this.name);
            }
        }
    }



    public List<Map<String, Object>> selectRows(Map<String, Object> conditions) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            if (matchesConditions(row, conditions)) {
                result.add(row);
            }

        }

        return result;
    }

    private boolean matchesConditions(Map<String, Object> row, Map<String, Object> conditions) {
        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String column = condition.getKey();
            Object value = condition.getValue();

            if (!row.containsKey(column) || !row.get(column).equals(value)) {
                return false;
            }
        }
        return true;
    }

    public void printResults(List<Map<String, Object>> result, List<String> colNames) {
        if (result.isEmpty()) {
            System.out.println("Запрос не вернул результатов.");
            return;
        }

        System.out.println("Результаты запроса:");

        System.out.println("────────────────────────────────────────────────────");

        for (String colName : colNames) {
            System.out.print(colName + "\t");
        }
        System.out.println("\n────────────────────────────────────────────────────");

        for (Map<String, Object> row : result) {
            for (String colName : colNames) {
                System.out.print(row.getOrDefault(colName, "NULL") + "\t");
            }
            System.out.println();
        }
        System.out.println("────────────────────────────────────────────────────");

    }

    public void normalizeRowTypes() {
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {

                String columnName = entry.getKey();
                Column column = columns.get(columnName);
                String type = columns.get(columnName).getType().toLowerCase();

                Object value = entry.getValue();

                if (entry.getValue() instanceof Double) {
                    double d = (Double) value;
                    if (d == (int) d) {
                        Object oldValue = entry.getValue();
                        Object newValue = (int) d;

                        entry.setValue((int) d);


                        if (column.getIsUnique()) {
                            updateUniqueValue(columnName, oldValue, newValue);
                        }
                    }
                } else if (value instanceof String && type.equals("date")) {
                    try {
                        ZonedDateTime zdt = ZonedDateTime.parse((String) value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        Object oldValue = entry.getValue();
                        entry.setValue(zdt);

                        if (column.getIsUnique()) {
                            updateUniqueValue(columnName, oldValue, zdt);
                        }

                    } catch (DateTimeParseException e) {
                        logger.severe("Ошибка преобразования даты в колонке " + columnName + ": " + e.getMessage());
                    }
                }
            }
        }

    }

    private void updateUniqueValue(String columnName, Object oldValue, Object newValue) {
        Set<Object> valuesSet = uniqueValues.get(columnName);
        if (valuesSet != null) {
            valuesSet.remove(oldValue);
            valuesSet.add(newValue);
        }
    }

}