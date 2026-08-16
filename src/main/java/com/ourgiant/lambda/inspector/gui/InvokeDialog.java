package com.ourgiant.lambda.inspector.gui;

import com.ourgiant.lambda.inspector.core.InvokeLogParser;
import com.ourgiant.lambda.inspector.core.InvokeRequests;
import com.ourgiant.lambda.inspector.core.PayloadValidation;
import com.ourgiant.lambda.inspector.core.ResponseFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.Optional;

/**
 * Test-invoke a function with an editable JSON payload, showing duration/billed-duration/
 * cold-start (parsed from the execution log tail - see core.InvokeLogParser) and the response
 * payload inline. This is the app's only mutating AWS action (see README Scope), so the actual
 * call always runs off the EDT in a SwingWorker.
 */
public class InvokeDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(InvokeDialog.class);

    private final LambdaClient lambdaClient;
    private final String functionName;

    private JTextArea payloadArea;
    private JButton invokeButton;
    private JPanel resultsContainer;
    private JLabel statusLine;
    private JPanel errorBanner;
    private JLabel errorBannerLabel;
    private JTextArea responseArea;

    public InvokeDialog(Frame owner, LambdaClient lambdaClient, String functionName) {
        super(owner, "Invoke: " + functionName, true);
        this.lambdaClient = lambdaClient;
        this.functionName = functionName;

        setSize(640, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        add(buildPayloadPanel(), BorderLayout.NORTH);
        add(new JScrollPane(buildResultsPanel()), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildPayloadPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        panel.add(new JLabel("Payload (JSON):"), BorderLayout.NORTH);

        payloadArea = new JTextArea("{}", 8, 40);
        payloadArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(payloadArea), BorderLayout.CENTER);

        invokeButton = new JButton("Invoke");
        invokeButton.addActionListener(e -> invoke());
        JPanel invokeRow = new JPanel(new BorderLayout());
        invokeRow.add(invokeButton, BorderLayout.EAST);
        panel.add(invokeRow, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildResultsPanel() {
        resultsContainer = new JPanel(new BorderLayout(4, 8));
        resultsContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        errorBanner = new JPanel(new BorderLayout());
        errorBanner.setBackground(new Color(255, 224, 224));
        errorBanner.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        errorBannerLabel = new JLabel(" ");
        errorBanner.add(errorBannerLabel, BorderLayout.CENTER);
        errorBanner.setVisible(false);

        statusLine = new JLabel(" ");

        JPanel topSection = new JPanel(new BorderLayout(4, 4));
        topSection.add(errorBanner, BorderLayout.NORTH);
        topSection.add(statusLine, BorderLayout.SOUTH);
        resultsContainer.add(topSection, BorderLayout.NORTH);

        responseArea = new JTextArea();
        responseArea.setEditable(false);
        responseArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        JScrollPane responseScroll = new JScrollPane(responseArea);
        responseScroll.setPreferredSize(new Dimension(560, 260));
        resultsContainer.add(responseScroll, BorderLayout.CENTER);

        return resultsContainer;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel wrapper = new JPanel();
        wrapper.add(closeButton);
        panel.add(wrapper);
        return panel;
    }

    private void invoke() {
        String payload = payloadArea.getText();
        Optional<String> validationError = PayloadValidation.validate(payload);
        if (validationError.isPresent()) {
            JOptionPane.showMessageDialog(this,
                "Payload is not valid JSON: " + validationError.get(),
                "Invalid Payload", JOptionPane.ERROR_MESSAGE);
            return;
        }

        invokeButton.setEnabled(false);
        statusLine.setText("Invoking...");
        errorBanner.setVisible(false);
        responseArea.setText("");

        new SwingWorker<InvokeResponse, Void>() {
            @Override
            protected InvokeResponse doInBackground() {
                return lambdaClient.invoke(InvokeRequests.build(functionName, payload));
            }

            @Override
            protected void done() {
                invokeButton.setEnabled(true);
                try {
                    InvokeResponse response = get();
                    displayResult(response);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    log.error("Error invoking function {}", functionName, cause);
                    JOptionPane.showMessageDialog(InvokeDialog.this,
                        "Error invoking function: " + cause.getMessage(),
                        "Invoke Error", JOptionPane.ERROR_MESSAGE);
                    statusLine.setText(" ");
                }
            }
        }.execute();
    }

    private void displayResult(InvokeResponse response) {
        Optional<InvokeLogParser.ExecutionReport> report = InvokeLogParser.parseBase64(response.logResult());

        StringBuilder status = new StringBuilder();
        status.append("<html>");
        status.append("<b>Status Code:</b> ").append(response.statusCode()).append("&nbsp;&nbsp;&nbsp;");
        if (report.isPresent()) {
            InvokeLogParser.ExecutionReport r = report.get();
            status.append("<b>Cold Start:</b> ").append(r.isColdStart() ? "Yes" : "No").append("&nbsp;&nbsp;&nbsp;");
            status.append("<b>Duration:</b> ").append(r.durationMs()).append(" ms&nbsp;&nbsp;&nbsp;");
            status.append("<b>Billed Duration:</b> ").append(r.billedDurationMs()).append(" ms&nbsp;&nbsp;&nbsp;");
            status.append("<b>Memory:</b> ").append(r.maxMemoryUsedMb()).append(" / ").append(r.memorySizeMb()).append(" MB");
        } else {
            status.append("<i>(execution metrics unavailable)</i>");
        }
        status.append("</html>");
        statusLine.setText(status.toString());

        if (response.functionError() != null) {
            errorBannerLabel.setText("<html><b>⚠ Function returned an error: " + response.functionError() + "</b></html>");
            errorBanner.setVisible(true);
        } else {
            errorBanner.setVisible(false);
        }

        String rawPayload = response.payload() != null ? response.payload().asUtf8String() : "";
        responseArea.setText(ResponseFormatter.prettyPrintOrRaw(rawPayload));
        responseArea.setCaretPosition(0);
    }
}
