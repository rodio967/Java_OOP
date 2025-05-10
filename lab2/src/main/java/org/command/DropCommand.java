package org.command;

import org.database.DatabaseManager;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DropCommand implements  SQLCommand {
    private final Pattern pattern = Pattern.compile("DROP TABLE (\\w+);");

    @Override
    public boolean matches(String command) {
        return pattern.matcher(command).matches();
    }

    @Override
    public void execute(String command, DatabaseManager dbManager) throws Exception {
        Matcher matcher = pattern.matcher(command);

        matcher.matches();

        String tableName = matcher.group(1);

        dbManager.dropTable(tableName);
        Files.deleteIfExists(Paths.get(dbManager.getDB_PATH() + tableName + ".db"));
    }
}
