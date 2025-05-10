package org.command;

import org.app.LoggerManager;
import org.database.DatabaseManager;
import org.model.Column;
import org.parser.SQLParser;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateCommand implements SQLCommand {
    private static final Logger logger = LoggerManager.getLogger(CreateCommand.class);
    private final Pattern pattern = Pattern.compile("CREATE TABLE (\\w+) \\((.*)\\);", Pattern.DOTALL);

    @Override
    public boolean matches(String command) {
        return pattern.matcher(command).matches();
    }

    @Override
    public void execute(String command, DatabaseManager dbManager) throws Exception {
        Matcher matcher = pattern.matcher(command);

        matcher.matches();

        String tableName = matcher.group(1);
        String columnsStr = matcher.group(2);


        List<Column> columns = SQLParser.parseColumns(columnsStr);
        dbManager.createTable(tableName, columns);

        dbManager.saveTable(tableName, dbManager.getTable(tableName));

        logger.info("Создана таблица " + tableName);
    }
}
