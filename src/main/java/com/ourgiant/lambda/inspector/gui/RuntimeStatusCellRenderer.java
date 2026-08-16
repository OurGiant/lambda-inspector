package com.ourgiant.lambda.inspector.gui;

import com.ourgiant.lambda.inspector.core.RuntimeLifecycle;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.time.LocalDate;

/**
 * Colors the Runtime column so an end-of-life runtime is visible at a glance across the whole
 * functions grid, not just when a single function's detail view is open - mirrors the
 * green/red "● Active"/"● Inactive" dot convention from the connection dialog's
 * profile status label.
 */
public class RuntimeStatusCellRenderer extends DefaultTableCellRenderer {

    private static final Color DEPRECATED_COLOR = Color.RED;
    private static final Color APPROACHING_EOL_COLOR = new Color(200, 120, 0);
    private static final Color UNKNOWN_COLOR = Color.GRAY;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!(c instanceof JLabel label) || !(value instanceof String runtimeId)) {
            return c;
        }

        RuntimeLifecycle.Status status = RuntimeLifecycle.classify(runtimeId, LocalDate.now());
        if (isSelected) {
            // Leave the look-and-feel's selection foreground alone rather than clashing with it.
            label.setText(runtimeId);
            return label;
        }

        switch (status) {
            case DEPRECATED -> {
                label.setForeground(DEPRECATED_COLOR);
                label.setText("● " + runtimeId);
            }
            case APPROACHING_EOL -> {
                label.setForeground(APPROACHING_EOL_COLOR);
                label.setText("● " + runtimeId);
            }
            case UNKNOWN -> {
                label.setForeground(UNKNOWN_COLOR);
                label.setText(runtimeId);
            }
            default -> {
                label.setForeground(table.getForeground());
                label.setText(runtimeId);
            }
        }
        return label;
    }
}
