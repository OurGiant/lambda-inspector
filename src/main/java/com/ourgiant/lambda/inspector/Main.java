package com.ourgiant.lambda.inspector;

import com.ourgiant.lambda.inspector.gui.MainWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        log.info("Starting Lambda Inspector...");

        SwingUtilities.invokeLater(() -> {
            try {
                log.debug("Initializing UI...");
                if (!ThemeManager.applyTheme("Flat Light")) {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }

                MainWindow window = new MainWindow();
                window.setVisible(true);
                log.info("Application started successfully.");

            } catch (Exception e) {
                log.error("Fatal error starting application", e);
                JOptionPane.showMessageDialog(null,
                    "Error starting application: " + e.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
