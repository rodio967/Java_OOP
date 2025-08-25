package calculator.commands;

import calculator.Context;

public class SQRTCommand implements Command{
    @Override
    public void execute(Context ctx, String[] args) {
        ctx.push(Math.sqrt(ctx.pop()));
    }
}
