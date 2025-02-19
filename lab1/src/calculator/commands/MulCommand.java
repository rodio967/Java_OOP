package calculator.commands;

import calculator.Context;

public class MulCommand implements Command{
    @Override
    public void execute(Context ctx, String[] args) {
        ctx.push(ctx.pop() * ctx.pop());
    }
}
