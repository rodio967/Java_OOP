package calculator.commands;

import calculator.Context;

public class PopCommand implements Command {
    @Override
    public void execute(Context ctx, String[] args) {
        ctx.pop();
    }
}
