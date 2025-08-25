package calculator.commands;

import calculator.Context;

public class DivCommand implements Command{
    @Override
    public void execute(Context ctx, String[] args) {
        double tmp1 = ctx.pop();
        double tmp2 = ctx.pop();
        if (tmp1 == 0) throw new ArithmeticException("Division by zero");
        ctx.push(tmp2 / tmp1);
    }
}
