package org.database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

class SQLParser {
    private final DatabaseManager dbManager;

    SQLParser(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    void execute(String command) throws Exception {
        command = command.trim().toUpperCase();

        Pattern createPattern = Pattern.compile(
                "CREATE TABLE (\\w+) \\((.*)\\);", Pattern.DOTALL);
        Matcher matcher = createPattern.matcher(command);

        if (matcher.matches()) {
            String tableName = matcher.group(1);
            String columnsStr = matcher.group(2);


            List<Column> columns = parseColumns(columnsStr);
            dbManager.createTable(tableName, columns);

            dbManager.saveTable(tableName, dbManager.getTable(tableName));

            System.out.println("Создана таблица " + tableName);

            return;
        }



        Pattern insertPattern = Pattern.compile(
                "INSERT INTO (\\w+)\\s*\\((.*)\\);", Pattern.DOTALL
        );


        matcher = insertPattern.matcher(command);
        if (matcher.matches()) {
            String tableName = matcher.group(1);
            String valuesBlock = matcher.group(2);

            Table table = dbManager.getTable(tableName);

            boolean hasBrackets = valuesBlock.trim().startsWith("(");

            if (hasBrackets) {
                Pattern rowPattern = Pattern.compile("\\((.*?)\\)");
                Matcher rowMatcher = rowPattern.matcher(valuesBlock);

                while (rowMatcher.find()) {
                    String rowValues = rowMatcher.group(1);
                    insertRowIntoTable(table, rowValues);
                }
            } else {

                insertRowIntoTable(table, valuesBlock);
            }
            dbManager.saveTable(table.getName(), table);


            return;
        }



        Pattern dropPattern = Pattern.compile("DROP TABLE (\\w+);");
        matcher = dropPattern.matcher(command);
        if (matcher.matches()) {
            String tableName = matcher.group(1);

            dbManager.dropTable(tableName);
            Files.deleteIfExists(Paths.get(dbManager.getDB_PATH() + tableName + ".db"));
            return;
        }

        Pattern selectPattern = Pattern.compile(
                "SELECT (.+) FROM (\\w+)(?: WHERE (.+?))?(?: SORT (\\w+))?;", Pattern.DOTALL);
        matcher = selectPattern.matcher(command);
        if (matcher.matches()) {
            String fields = matcher.group(1);


            String tableName = matcher.group(2);
            String whereClause = matcher.group(3);
            String sortColumn = matcher.group(4);

            Table table = dbManager.getTable(tableName);

            List<Map<String, Object>> result;
            if (whereClause != null) {
                Map<String, Object> conditions = parseConditions(whereClause, table);
                result = table.selectRows(conditions);
            } else {
                result = table.getRows();
            }


            List<String> colNames;
            if (fields.equals("*")) {
                colNames = table.getColumnNames();
            } else {
                colNames = new ArrayList<>();
                String[] columns = fields.split("\\s*,\\s*");

                for (String column : columns) {
                    if (!table.getColumns().containsKey(column)) {
                        throw new IllegalArgumentException("В таблице " + tableName + " нет поля " + column);
                    }
                    colNames.add(column);
                }
            }

            if (sortColumn != null) {
                if (!table.getColumns().containsKey(sortColumn)) {
                    throw new IllegalArgumentException("В таблице " + tableName + " нет поля " + sortColumn);
                }

                result.sort((row1, row2) -> {
                    Object v1 = row1.get(sortColumn);
                    Object v2 = row2.get(sortColumn);

                    if (v1 instanceof Comparable && v2 instanceof Comparable) {
                        return ((Comparable) v1).compareTo(v2);
                    }
                    return 0;
                });
            }

            table.printResults(result, colNames);

            return;
        }


        Pattern deletePattern = Pattern.compile(
                "DELETE FROM (\\w+) WHERE (.+);", Pattern.DOTALL);
        matcher = deletePattern.matcher(command);

        if (matcher.matches()) {
            String tableName = matcher.group(1);
            String conditionStr = matcher.group(2);

            Table table = dbManager.getTable(tableName);


            Map<String, Object> conditions = parseConditions(conditionStr, table);
            table.deleteRows(conditions);
            dbManager.saveTable(tableName, table);
            return;
        }


        throw new Exception("Unknown command: " + command);
    }

    private Map<String, Object> parseConditions(String conditionStr, Table table) {
        Map<String, Object> conditions = new HashMap<>();

        String[] conditionsArray = conditionStr.split("\\s+AND\\s+");
        for (String condition : conditionsArray) {
            String[] keyValue = condition.split("=");
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Ошибка в условии " + condition);
            }

            String column = keyValue[0].trim();
            String value = keyValue[1].trim();

            conditions.put(column, parseValue(value, table.getColumn(column).getType()));


        }
        return conditions;
    }


    private List<Column> parseColumns(String columnsStr) {
        List<Column> columns = new ArrayList<>();
        String[] colDefs = columnsStr.split(";");
        for (String colDef : colDefs) {
            colDef = colDef.trim();
            if(colDef.isEmpty()) {
                continue;
            }


            String[] parts = colDef.split(" ");

            if (parts.length < 2) {
                System.out.println("ошибка:" + colDef);
                throw new IllegalArgumentException("Ошибка в объявлении столбца: " + colDef);
            }

            String name = parts[0];
            String type = parts[1];
            boolean isUnique = Arrays.asList(parts).contains("UNIQUE");
            boolean isNotNull = Arrays.asList(parts).contains("NOT-NULL");
            columns.add(new Column(name, type, isUnique, isNotNull));
        }
        return columns;
    }


    private Object parseValue(String value, String expectedType) {

        value = value.trim();

        if (value.isEmpty()) {
            return value;
        }

        switch (expectedType.toLowerCase()) {
            case "int":
                if (!value.matches("-?\\d+")) {
                    throw new IllegalArgumentException("Ошибка: '" + value + "' не является целым числом.");
                }
                return Integer.parseInt(value);
            case "boolean":
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("Ошибка: '" + value + "' не является значением true/false.");
                }
                return Boolean.parseBoolean(value);
            case "string":
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    return value.substring(1, value.length() - 1).trim();
                }
                throw new IllegalArgumentException("Ошибка: '" + value + "' не является строкой ");
            default:
                throw new IllegalArgumentException("Неизвестный тип данных: " + expectedType);
        }
    }

    private void insertRowIntoTable(Table table, String rowValues) throws Exception {
        String[] values = rowValues.split("\\s*,\\s*");

        if (values.length != table.getColumns().size()) {
            throw new IllegalArgumentException("Ошибка: количество значений не совпадает с количеством столбцов.");
        }

        Map<String, Object> row = new HashMap<>();
        int i = 0;
        for (Column column : table.getColumns().values()) {
            String value = values[i];


            row.put(column.getName(), parseValue(value, column.getType()));
            i++;
        }
        table.insertRow(row);
        System.out.println("Данные добавлены в таблицу " + table.getName());
    }

}
