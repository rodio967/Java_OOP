package org.command;

import org.database.DatabaseManager;

public interface SQLCommand {
    boolean matches(String command);
    void execute(String command, DatabaseManager dbManager) throws Exception;
}
