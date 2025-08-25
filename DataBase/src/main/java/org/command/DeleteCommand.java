package org.command;

import org.database.DatabaseManager;
import org.model.Table;
import org.parser.SQLParser;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteCommand implements SQLCommand {
    private final Pattern pattern = Pattern.compile("DELETE FROM (\\w+) WHERE (.+);", Pattern.DOTALL);

    @Override
    public boolean matches(String command) {
        return pattern.matcher(command).matches();
    }

    @Override
    public void execute(String command, DatabaseManager dbManager) throws Exception {
        Matcher matcher = pattern.matcher(command);

        matcher.matches();

        String tableName = matcher.group(1);
        String conditionStr = matcher.group(2);

        Table table = dbManager.getTable(tableName);

        Map<String, Object> conditions = SQLParser.parseConditions(conditionStr, table);
        table.deleteRows(conditions);
        dbManager.saveTable(tableName, table);
    }
}
