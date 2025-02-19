package calculator.commands;

import calculator.Context;

public interface Command {
    void execute(Context ctx, String[] args);
}