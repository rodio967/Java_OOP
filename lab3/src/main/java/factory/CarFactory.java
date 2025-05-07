package factory;

import factory.gui.FactoryGUI;

import javax.swing.*;
import java.io.IOException;

public class CarFactory {
    public static void main(String[] args) {
        try {
            FactoryConfig config = FactoryConfig.loadFromFile("factory.properties");
            SwingUtilities.invokeLater(() -> {
                FactoryGUI gui = new FactoryGUI(config);
                gui.setVisible(true);

                gui.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                        gui.shutdown();
                    }
                });
            });
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            System.exit(1);
        }
    }
}
