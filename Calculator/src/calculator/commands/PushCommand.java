package calculator.commands;

import calculator.Context;

public class PushCommand implements Command {
    @Override
    public void execute(Context ctx, String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("PUSH требует аргумент");
        }
        double value;
        if (args[1].matches("-?\\d+(\\.\\d+)?")) {
            value = Double.parseDouble(args[1]);
        } else {
            value = ctx.getVariable(args[1]);
        }
        ctx.push(value);
    }
}
