package org.parser;

import org.command.SQLCommand;
import org.database.DatabaseManager;
import org.app.LoggerManager;
import org.model.Column;
import org.model.Table;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class SQLParser {
    private static final Logger logger = LoggerManager.getLogger(SQLParser.class);
    private final DatabaseManager dbManager;
    private static final Map<String, String> arrayTypeMap = new HashMap<>();
    private static final Map<String, Function<String, Object>> typeParsers = new HashMap<>();

    static {
        initializeMaps();
    }


    public SQLParser(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void execute(String command) throws Exception {
        command = command.trim().toUpperCase();

        String commandKey = command.trim().split("\\s+")[0].toUpperCase();
        SQLCommand sqlCommand = SQLCommandFactory.getCommand(commandKey);

        if (sqlCommand != null && sqlCommand.matches(command)) {
            sqlCommand.execute(command, dbManager);
        } else {
            throw new IllegalArgumentException("Неизвестная или неподдерживаемая команда: " + command);
        }

    }

    public static Map<String, Object> parseConditions(String conditionStr, Table table) {
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


    public static List<Column> parseColumns(String columnsStr) {
        List<Column> columns = new ArrayList<>();
        String[] colDefs = columnsStr.split(";");
        for (String colDef : colDefs) {
            colDef = colDef.trim();

            if(colDef.isEmpty()) {
                continue;
            }

            columns.add(parseColumn(colDef));
        }
        return columns;
    }

    public static Column parseColumn(String colDef) {
        String[] parts = colDef.split("\\s+");

        if (parts.length < 2) {
            logger.info("ошибка:" + colDef);
            throw new IllegalArgumentException("Ошибка в объявлении столбца: " + colDef);
        }

        String name = parts[0];
        String type = parts[1];
        boolean isUnique = Arrays.asList(parts).contains("UNIQUE");
        boolean isNotNull = Arrays.asList(parts).contains("NOT-NULL");

        return new Column(name, type, isUnique, isNotNull);
    }


    public static Object parseValue(String value, String expectedType) {

        value = value.trim();

        if (value.isEmpty()) {
            return value;
        }


        if (isArray(expectedType)) {
            if (!value.startsWith("[") || !value.endsWith("]")) {
                throw new IllegalArgumentException("Ошибка: значение \"" + value + "\" не является массивом ");
            }
            String type_ = getString(expectedType);

            String subValue_ = value.substring(1, value.length() - 1);

            String[] elems = subValue_.split("\\s+");

            List<Object> array = new ArrayList<>();

            for (String elem : elems) {
                Object typedValue = checkAndParseValue(elem, type_);
                array.add(typedValue);
            }
            return array;
        }

        return checkAndParseValue(value, expectedType);
    }

    private static boolean isArray(String expectedType) {
        return expectedType.startsWith("[]");
    }


    private static String getString(String expectedType) {
        String expectedType_ = expectedType.substring(2).trim().toLowerCase();;
        String result = arrayTypeMap.get(expectedType_);

        if (result == null) {
            throw new IllegalArgumentException("Ошибка: неверно указан тип после [] – " + expectedType);
        }
        return result;
    }



    public static Map<String, Object> parseRow(Table table, String rowValues) throws Exception {
        String[] values = rowValues.split("\\s*,\\s*");

        if (values.length != table.getColumns().size()) {
            throw new IllegalArgumentException("Ошибка: количество значений не совпадает с количеством столбцов.");
        }

        Map<String, Object> row = new HashMap<>();
        Iterator<String> valueIterator = Arrays.asList(values).iterator();

        for (Column column : table.getColumns().values()) {
            String value = valueIterator.next();
            row.put(column.getName(), parseValue(value, column.getType()));
        }

        return row;
    }

    public static void initializeMaps() {
        arrayTypeMap.put("strings", "string");
        arrayTypeMap.put("ints", "int");
        arrayTypeMap.put("booleans", "boolean");
        arrayTypeMap.put("dates", "date");


        typeParsers.put("string", value -> {
            value = value.trim();
            if (value.startsWith("\"") && value.endsWith("\"")) {
                return value.substring(1, value.length() - 1).trim();
            }
            throw new IllegalArgumentException("Строка должна быть в кавычках: " + value);
        });

        typeParsers.put("int", value -> {
            value = value.trim();
            if (value.matches("-?\\d+")) {
                return Integer.parseInt(value);
            }
            throw new IllegalArgumentException("Неверное целочисленное значение: " + value);
        });

        typeParsers.put("boolean", value -> {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(value);
            }
            throw new IllegalArgumentException("Неверное логическое значение: " + value);
        });

        typeParsers.put("date", value -> {
            value = value.trim().replaceAll("^\"|\"$", "");
            try {
                return ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Неверный формат даты. Используй ISO, например: 2025-02-19T21:43:15+0000");
            }
        });
    }


    private static Object checkAndParseValue(String value, String type_) {
        value = value.trim();

        String typeKey = type_.toLowerCase();
        Function<String, Object> parser = typeParsers.get(typeKey);

        if (parser == null) {
            throw new IllegalArgumentException("Неизвестный тип: " + type_);
        }

        return parser.apply(value);
    }


}
