package calculator.commands;

import calculator.Context;

public class PrintCommand implements Command {
    @Override
    public void execute(Context ctx, String[] args) {
        System.out.println(ctx.peek());
    }
}
