package com.ourgiant.lambda.inspector.core;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Pure parsing of a Lambda invocation's execution log tail (the base64-encoded `logResult` field
// on InvokeResponse, present when the request used LogType.TAIL - see InvokeRequests) into the
// duration/memory/cold-start figures the invoke dialog shows. No javax.swing.* dependency.
public final class InvokeLogParser {

    // Example REPORT line:
    // REPORT RequestId: 8f2a1b3c-...  Duration: 12.34 ms  Billed Duration: 13 ms
    // Memory Size: 256 MB  Max Memory Used: 45 MB  Init Duration: 150.23 ms
    // Init Duration is present ONLY on a cold start (a fresh execution environment) - its
    // presence/absence is exactly what ExecutionReport.isColdStart() reports; there is no
    // separate cold-start signal to derive it from.
    private static final Pattern REPORT_LINE = Pattern.compile(
        "REPORT RequestId:\\s*(\\S+)\\s+"
            + "Duration:\\s*([\\d.]+)\\s*ms\\s+"
            + "Billed Duration:\\s*(\\d+)\\s*ms\\s+"
            + "Memory Size:\\s*(\\d+)\\s*MB\\s+"
            + "Max Memory Used:\\s*(\\d+)\\s*MB"
            + "(?:\\s+Init Duration:\\s*([\\d.]+)\\s*ms)?");

    private InvokeLogParser() {
    }

    public record ExecutionReport(
        double durationMs,
        int billedDurationMs,
        int memorySizeMb,
        int maxMemoryUsedMb,
        Double initDurationMs) {

        public boolean isColdStart() {
            return initDurationMs != null;
        }
    }

    /** Parses an already-decoded execution log tail. Returns empty if no REPORT line is found. */
    public static Optional<ExecutionReport> parse(String decodedLogText) {
        if (decodedLogText == null || decodedLogText.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = REPORT_LINE.matcher(decodedLogText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        double duration = Double.parseDouble(matcher.group(2));
        int billedDuration = Integer.parseInt(matcher.group(3));
        int memorySize = Integer.parseInt(matcher.group(4));
        int maxMemoryUsed = Integer.parseInt(matcher.group(5));
        Double initDuration = matcher.group(6) != null ? Double.parseDouble(matcher.group(6)) : null;
        return Optional.of(new ExecutionReport(duration, billedDuration, memorySize, maxMemoryUsed, initDuration));
    }

    /** Decodes InvokeResponse.logResult() (base64) and delegates to parse(). */
    public static Optional<ExecutionReport> parseBase64(String rawLogResultField) {
        if (rawLogResultField == null || rawLogResultField.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(rawLogResultField);
            return parse(new String(decoded, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
