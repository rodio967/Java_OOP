package org.command;

import org.database.DatabaseManager;
import org.model.Table;
import org.parser.SQLParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelectCommand implements  SQLCommand {
    private final Pattern pattern = Pattern.compile("SELECT (.+) FROM (\\w+)(?: WHERE (.+?))?(?: SORT (\\w+))?;", Pattern.DOTALL);

    @Override
    public boolean matches(String command) {
        return pattern.matcher(command).matches();
    }

    @Override
    public void execute(String command, DatabaseManager dbManager) throws Exception {
        Matcher matcher = pattern.matcher(command);

        matcher.matches();

        String fields = matcher.group(1);


        String tableName = matcher.group(2);
        String whereClause = matcher.group(3);
        String sortColumn = matcher.group(4);

        Table table = dbManager.getTable(tableName);

        List<Map<String, Object>> result;
        if (whereClause != null) {
            Map<String, Object> conditions = SQLParser.parseConditions(whereClause, table);
            result = table.selectRows(conditions);
        } else {
            result = new ArrayList<>(table.getRows());
        }

        List<String> colNames = getStringList(fields, table, tableName);

        if (sortColumn != null) {
            sortResults(result, sortColumn, table, tableName);
        }

        table.printResults(result, colNames);
    }

    private static List<String> getStringList(String fields, Table table, String tableName) {
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
        return colNames;
    }

    private static void sortResults(List<Map<String, Object>> result, String sortColumn, Table table, String tableName) {
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
}
