package org.command;

import org.app.LoggerManager;
import org.database.DatabaseManager;
import org.model.Table;
import org.parser.SQLParser;

import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InsertCommand implements SQLCommand {
    private static final Logger logger = LoggerManager.getLogger(InsertCommand.class);
    private final Pattern pattern = Pattern.compile("INSERT INTO (\\w+)\\s*\\((.*)\\);", Pattern.DOTALL);

    @Override
    public boolean matches(String command) {
        return pattern.matcher(command).matches();
    }

    @Override
    public void execute(String command, DatabaseManager dbManager) throws Exception {
        Matcher matcher = pattern.matcher(command);

        matcher.matches();

        String tableName = matcher.group(1);
        String valuesBlock = matcher.group(2);

        Table table = dbManager.getTable(tableName);

        boolean hasBrackets = valuesBlock.trim().startsWith("(");

        if (hasBrackets) {
            Pattern rowPattern = Pattern.compile("\\((.*?)\\)");
            Matcher rowMatcher = rowPattern.matcher(valuesBlock);

            while (rowMatcher.find()) {
                String rowValues = rowMatcher.group(1);
                Map<String, Object> row = SQLParser.parseRow(table, rowValues);
                table.insertRow(row);
            }
        } else {
            Map<String, Object> row = SQLParser.parseRow(table, valuesBlock);
            table.insertRow(row);
        }
        dbManager.saveTable(table.getName(), table);
        logger.info("Данные добавлены в таблицу " + table.getName());
    }

}
