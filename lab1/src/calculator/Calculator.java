package calculator;

import calculator.commands.Command;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Calculator {
    public static void main(String[] args) {
        Context context = new Context();

        try (BufferedReader reader = args.length > 0 ?
                new BufferedReader(new FileReader(args[0])) :
                new BufferedReader(new InputStreamReader(System.in))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(" ");
                Command command = CommandFactory.getCommand(parts[0]);
                command.execute(context, parts);
            }
        } catch (IOException e) {
            System.err.println("Ошибка ввода: " + e.getMessage());
        }
    }
}
