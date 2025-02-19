package calculator.commands;

import calculator.Context;

public class AddCommand implements Command {
    @Override
    public void execute(Context ctx, String[] args) {
        ctx.push(ctx.pop() + ctx.pop());
    }
}
