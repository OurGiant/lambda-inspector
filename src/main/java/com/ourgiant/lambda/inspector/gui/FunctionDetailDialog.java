package com.ourgiant.lambda.inspector.gui;

import com.ourgiant.lambda.inspector.core.EventSourceMappingTriggers;
import com.ourgiant.lambda.inspector.core.ResourcePolicyTriggers;
import com.ourgiant.lambda.inspector.core.RuntimeLifecycle;
import com.ourgiant.lambda.inspector.core.TriggerRequests;
import com.ourgiant.lambda.inspector.model.FunctionSummary;
import com.ourgiant.lambda.inspector.model.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.GetPolicyResponse;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsResponse;
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-only detail view for a single function's configuration - everything here comes from the
 * same ListFunctions call that populates the grid (see core.FunctionGridModel), so opening this
 * dialog makes no additional AWS API call. Shows a call-to-action banner when the function's
 * runtime is deprecated or approaching end of life (see core.RuntimeLifecycle).
 */
public class FunctionDetailDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(FunctionDetailDialog.class);
    private static final String MASK = "••••••••";

    private JLabel triggersStatusLabel;
    private DefaultTableModel triggersTableModel;

    public FunctionDetailDialog(Frame owner, LambdaClient lambdaClient, FunctionSummary summary) {
        super(owner, "Function: " + safe(summary.functionName), true);
        setSize(640, 680);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        RuntimeLifecycle.Status status = RuntimeLifecycle.classify(summary.runtime, LocalDate.now());

        JPanel banner = buildEolBanner(summary.runtime, status);
        if (banner != null) {
            add(banner, BorderLayout.NORTH);
        }

        add(new JScrollPane(buildBody(summary, status)), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        loadTriggers(lambdaClient, summary.functionName);
    }

    private JPanel buildEolBanner(String runtimeId, RuntimeLifecycle.Status status) {
        String message = RuntimeLifecycle.bannerMessage(runtimeId, status, LocalDate.now());
        if (message == null) {
            return null;
        }

        JPanel panel = new JPanel(new BorderLayout(8, 4));
        Color background = status == RuntimeLifecycle.Status.DEPRECATED
            ? new Color(255, 224, 224)
            : new Color(255, 240, 214);
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel text = new JLabel("<html><b>⚠ " + message + "</b></html>");
        panel.add(text, BorderLayout.CENTER);

        JLabel link = new JLabel(RuntimeLifecycle.RUNTIME_DOCS_URL);
        link.setForeground(new Color(0, 0, 200));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setFont(link.getFont().deriveFont(Map.of(java.awt.font.TextAttribute.UNDERLINE,
            java.awt.font.TextAttribute.UNDERLINE_ON)));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(RuntimeLifecycle.RUNTIME_DOCS_URL));
                } catch (Exception ex) {
                    log.warn("Failed to open runtime docs URL", ex);
                }
            }
        });
        panel.add(link, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildBody(FunctionSummary summary, RuntimeLifecycle.Status status) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        int row = 0;

        row = addField(panel, gbc, row, "Function Name:", safe(summary.functionName));
        row = addField(panel, gbc, row, "Description:", safe(summary.description));
        row = addField(panel, gbc, row, "Runtime:", runtimeLabel(summary.runtime, status));
        row = addField(panel, gbc, row, "Handler:", safe(summary.handler));
        row = addField(panel, gbc, row, "Memory:", summary.memorySizeMb != null ? summary.memorySizeMb + " MB" : "—");
        row = addField(panel, gbc, row, "Timeout:", summary.timeoutSeconds != null ? summary.timeoutSeconds + " s" : "—");
        row = addField(panel, gbc, row, "Architecture:", safe(summary.architecture));
        row = addField(panel, gbc, row, "Last Modified:", safe(summary.lastModified));
        row = addField(panel, gbc, row, "IAM Role:", safe(summary.roleArn));
        row = addField(panel, gbc, row, "Layers:", listOrNone(summary.layerArns));

        if (hasVpcConfig(summary)) {
            row = addField(panel, gbc, row, "VPC Subnets:", listOrNone(summary.vpcSubnetIds));
            row = addField(panel, gbc, row, "VPC Security Groups:", listOrNone(summary.vpcSecurityGroupIds));
        }

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 0;
        panel.add(new JLabel("Environment Variables:"), gbc);
        row++;

        gbc.gridy = row;
        panel.add(buildEnvironmentPanel(summary.environmentVariables), gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Triggers:"), gbc);
        row++;

        gbc.gridy = row;
        panel.add(buildTriggersPanel(), gbc);

        return panel;
    }

    private JPanel buildTriggersPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        triggersStatusLabel = new JLabel("Loading triggers...");
        panel.add(triggersStatusLabel, BorderLayout.NORTH);

        triggersTableModel = new DefaultTableModel(new Object[]{"Trigger", "Source", "Detail"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable triggersTable = new JTable(triggersTableModel);
        JScrollPane scrollPane = new JScrollPane(triggersTable);
        scrollPane.setPreferredSize(new Dimension(560, 120));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Fetches triggers in the background so opening this dialog doesn't block on two extra AWS
     * calls beyond the ListFunctions data already shown - ListEventSourceMappings (poll-based
     * triggers: SQS, DynamoDB Streams, Kinesis, ...) and GetPolicy (push-based triggers: S3,
     * API Gateway, EventBridge, SNS, ... - shown as resource-policy grants, not event source
     * mappings). A function with no resource-based policy at all throws
     * ResourceNotFoundException from GetPolicy - that's normal (no push-based triggers), not
     * an error to surface.
     */
    private void loadTriggers(LambdaClient lambdaClient, String functionName) {
        new SwingWorker<List<Trigger>, Void>() {
            @Override
            protected List<Trigger> doInBackground() {
                List<Trigger> triggers = new ArrayList<>();
                try {
                    ListEventSourceMappingsResponse esmResponse = lambdaClient.listEventSourceMappings(
                        TriggerRequests.listEventSourceMappings(functionName));
                    triggers.addAll(EventSourceMappingTriggers.from(esmResponse.eventSourceMappings()));
                } catch (SdkException e) {
                    log.warn("Failed to list event source mappings for {}", functionName, e);
                }
                try {
                    GetPolicyResponse policyResponse = lambdaClient.getPolicy(
                        TriggerRequests.getPolicy(functionName));
                    triggers.addAll(ResourcePolicyTriggers.parse(policyResponse.policy()));
                } catch (ResourceNotFoundException e) {
                    // No resource-based policy at all - i.e. no push-based triggers. Not an error.
                } catch (SdkException e) {
                    log.warn("Failed to get resource policy for {}", functionName, e);
                }
                return triggers;
            }

            @Override
            protected void done() {
                List<Trigger> triggers;
                try {
                    triggers = get();
                } catch (Exception e) {
                    triggersStatusLabel.setText("Failed to load triggers: " + e.getMessage());
                    return;
                }
                if (triggers.isEmpty()) {
                    triggersStatusLabel.setText("(no triggers found)");
                    return;
                }
                triggersStatusLabel.setText(" ");
                for (Trigger trigger : triggers) {
                    triggersTableModel.addRow(new Object[]{
                        trigger.type, trigger.source, trigger.detail != null ? trigger.detail : "—"});
                }
            }
        }.execute();
    }

    private boolean hasVpcConfig(FunctionSummary summary) {
        return (summary.vpcSubnetIds != null && !summary.vpcSubnetIds.isEmpty())
            || (summary.vpcSecurityGroupIds != null && !summary.vpcSecurityGroupIds.isEmpty());
    }

    private String runtimeLabel(String runtimeId, RuntimeLifecycle.Status status) {
        String label = safe(runtimeId);
        return switch (status) {
            case DEPRECATED -> label + "  (deprecated)";
            case APPROACHING_EOL -> label + "  (approaching end of life)";
            case UNKNOWN -> label + "  (unrecognized runtime)";
            case SUPPORTED -> label;
        };
    }

    private int addField(JPanel panel, GridBagConstraints gbc, int row, String labelText, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        // `value` is expected to already be HTML-escaped (and, for list fields, to already
        // contain intentional <br> tags) - see safe()/listOrNone() below - so it isn't
        // re-escaped here, which would turn those <br> tags into literal visible text.
        JLabel valueLabel = new JLabel("<html>" + value + "</html>");
        panel.add(valueLabel, gbc);
        return row + 1;
    }

    private JPanel buildEnvironmentPanel(Map<String, String> environmentVariables) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        Map<String, String> vars = environmentVariables != null ? environmentVariables : Map.of();

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Key", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        JTable table = new JTable(model);
        boolean[] revealed = {false};
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int r, int c) {
                Object displayed = revealed[0] ? value : MASK;
                return super.getTableCellRendererComponent(t, displayed, isSelected, hasFocus, r, c);
            }
        });
        // JTable installs a default Ctrl+C TransferHandler that reads getValueAt() directly -
        // the real, unmasked model value - completely bypassing the renderer's mask above.
        // Without this override, masking is purely cosmetic: select a cell and copy, and the
        // real secret is on the clipboard regardless of whether "Show values" is checked.
        // This TransferHandler builds the clipboard text from what's actually on screen instead.
        table.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                JTable source = (JTable) c;
                int[] rows = source.getSelectedRows();
                int[] cols = source.getSelectedColumns();
                StringBuilder sb = new StringBuilder();
                for (int r : rows) {
                    for (int i = 0; i < cols.length; i++) {
                        Object value = source.getValueAt(r, cols[i]);
                        if (cols[i] == 1 && !revealed[0]) {
                            value = MASK;
                        }
                        sb.append(value);
                        if (i < cols.length - 1) {
                            sb.append('\t');
                        }
                    }
                    sb.append('\n');
                }
                return new StringSelection(sb.toString());
            }
        });

        JCheckBox revealToggle = new JCheckBox("Show values");
        revealToggle.setToolTipText("Environment variable values are masked by default since they often hold secrets.");
        revealToggle.addActionListener(e -> {
            revealed[0] = revealToggle.isSelected();
            table.repaint();
        });

        if (vars.isEmpty()) {
            panel.add(new JLabel("(no environment variables)"), BorderLayout.NORTH);
        } else {
            panel.add(revealToggle, BorderLayout.NORTH);
        }
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new java.awt.Dimension(560, 140));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private static String listOrNone(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "—";
        }
        return String.join("<br>", values.stream().map(FunctionDetailDialog::escape).toList());
    }

    private static String safe(String value) {
        return value != null && !value.isBlank() ? escape(value) : "—";
    }

    private static String escape(String value) {
        if (value == null) {
            return "—";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
