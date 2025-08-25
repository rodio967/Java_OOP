package calculator.commands;

import calculator.Context;

public class DefineCommand implements Command{
    @Override
    public void execute(Context ctx, String[] args) {
        ctx.define(args[1], Double.parseDouble(args[2]));
    }
}
