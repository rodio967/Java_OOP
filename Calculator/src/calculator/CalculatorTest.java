package calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import calculator.commands.Command;
import calculator.commands.AddCommand;
import calculator.commands.SubCommand;
import calculator.commands.MulCommand;
import calculator.commands.DivCommand;
import calculator.commands.SQRTCommand;


class CalculatorTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @Test
    void testPush() {
        context.push(10.5);
        assertEquals(10.5, context.pop(), 0.0001, "PUSH не работает корректно");
    }

    @Test
    void testPop() {
        context.push(5.0);
        double value = context.pop();
        assertEquals(5.0, value, 0.0001, "POP возвращает неправильное значение");
        assertThrows(RuntimeException.class, () -> context.pop(), "POP не выбрасывает исключение на пустом стеке");
    }

    @Test
    void testAdd() {
        context.push(2.0);
        context.push(3.0);
        Command command = new AddCommand();
        command.execute(context, new String[]{ "" });
        assertEquals(5.0, context.pop(), 0.0001, "Сложение работает некорректно");
    }

    @Test
    void testAdditionWithEmptyStack() {
        Context context = new Context();
        Command command = new AddCommand();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            command.execute(context, new String[]{ "+" });
        });

        assertEquals("Стек пуст", exception.getMessage());
    }

    @Test
    void testSub() {
        context.push(3.0);
        context.push(2.0);
        Command command = new SubCommand();
        command.execute(context, new String[]{ "" });
        assertEquals(1.0, context.pop(), 0.0001, "Вычитание работает некорректно");
    }

    @Test
    void testMul() {
        context.push(3.0);
        context.push(2.0);
        Command command = new MulCommand();
        command.execute(context, new String[]{ "" });
        assertEquals(6.0, context.pop(), 0.0001, "Умножение работает некорректно");
    }

    @Test
    void testDiv() {
        context.push(4.0);
        context.push(2.0);
        Command command = new DivCommand();
        command.execute(context, new String[]{ "" });
        assertEquals(2.0, context.pop(), 0.0001, "Деление работает некорректно");
    }

    @Test
    void testSQRT() {
        context.push(9.0);
        Command command = new SQRTCommand();
        command.execute(context, new String[]{ "" });
        assertEquals(3.0, context.pop(), 0.0001, "Корень работает некорректно");
    }

    @Test
    void testPrint() {
        context.push(42.0);
        assertEquals(42.0, context.peek(), 0.0001, "PRINT работает некорректно");
    }

    @Test
    void testDefine() {
        CommandFactory.getCommand("DEFINE").execute(context, new String[]{ "DEFINE", "x", "10" });
        assertEquals(10.0, context.getVariable("x"), 0.0001, "DEFINE работает некорректно");
    }
}