package calculator.commands;

import calculator.Context;

public class ExitCommand implements Command{
    @Override
    public void execute(Context ctx, String[] args) {
        System.exit(0);
    }
}
