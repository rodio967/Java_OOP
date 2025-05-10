package calculator.commands;

import calculator.Context;

public class SubCommand implements Command{
    @Override
    public void execute(Context ctx, String[] args) {
        double tmp = ctx.pop();
        ctx.push(ctx.pop() - tmp);
    }
}
