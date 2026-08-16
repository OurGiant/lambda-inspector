package com.ourgiant.lambda.inspector.core;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Pure logic (no javax.swing.*) for classifying an AWS Lambda runtime identifier's
 * end-of-life status, so the functions grid and the config detail view can both flag
 * deprecated/soon-to-be-deprecated runtimes without a live network call.
 *
 * <p>Data source: <a href="https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html">
 * Lambda runtimes</a> (Supported runtimes + Deprecated runtimes tables), fetched 2026-08-15.
 * AWS publishes these deprecation dates as "projected" / "subject to change" — this table will
 * go stale over time and should be refreshed periodically against that page, not treated as a
 * one-time snapshot.
 */
public final class RuntimeLifecycle {

    public enum Status {
        /** Actively supported, and not within the AWS-notification window of its deprecation date. */
        SUPPORTED,
        /** Within 180 days of its deprecation date (AWS's own documented customer-notice window). */
        APPROACHING_EOL,
        /** At or past its deprecation date — AWS no longer patches it. */
        DEPRECATED,
        /** Not in the known-runtime table (includes container-image functions, whose runtime is null). */
        UNKNOWN
    }

    /** AWS notifies accounts at least this many days before a runtime's deprecation date. */
    public static final int NOTICE_WINDOW_DAYS = 180;

    public static final String RUNTIME_DOCS_URL = "https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html";

    // Runtime identifier -> deprecation date. Absence of a key here (but presence in
    // NOT_SCHEDULED) means "supported, no deprecation date published yet" (e.g. preview
    // runtimes). Absence from *both* means the identifier is unknown to this table entirely.
    //
    // java8 / java8.al2 / java8.al2023 are three DISTINCT runtime identifiers with different
    // deprecation dates (the AL2023-based Java 8/11/17 runtimes follow a later schedule than
    // their AL2/AL1 predecessors) - same for provided / provided.al2 / provided.al2023. Do not
    // collapse these.
    private static final Map<String, LocalDate> DEPRECATION_DATES = new HashMap<>();

    // Runtimes AWS lists with "Not scheduled" deprecation (currently: preview runtimes).
    private static final Set<String> NOT_SCHEDULED = Set.of("nodejs26.x", "python3.15");

    static {
        // --- Currently supported (not yet deprecated) ---
        DEPRECATION_DATES.put("nodejs24.x", LocalDate.of(2028, 4, 30));
        DEPRECATION_DATES.put("nodejs22.x", LocalDate.of(2027, 4, 30));

        DEPRECATION_DATES.put("python3.14", LocalDate.of(2029, 6, 30));
        DEPRECATION_DATES.put("python3.13", LocalDate.of(2029, 6, 30));
        DEPRECATION_DATES.put("python3.12", LocalDate.of(2028, 10, 31));
        DEPRECATION_DATES.put("python3.11", LocalDate.of(2027, 6, 30));
        DEPRECATION_DATES.put("python3.10", LocalDate.of(2026, 10, 31));

        DEPRECATION_DATES.put("java25", LocalDate.of(2029, 6, 30));
        DEPRECATION_DATES.put("java21", LocalDate.of(2029, 6, 30));
        DEPRECATION_DATES.put("java17.al2023", LocalDate.of(2029, 6, 30));
        DEPRECATION_DATES.put("java11.al2023", LocalDate.of(2029, 6, 30));
        DEPRECATION_DATES.put("java8.al2023", LocalDate.of(2029, 6, 30));
        DEPRECATION_DATES.put("java17", LocalDate.of(2027, 6, 30));
        DEPRECATION_DATES.put("java11", LocalDate.of(2027, 6, 30));
        DEPRECATION_DATES.put("java8.al2", LocalDate.of(2027, 6, 30));

        DEPRECATION_DATES.put("dotnet10", LocalDate.of(2028, 11, 14));
        DEPRECATION_DATES.put("dotnet9", LocalDate.of(2026, 11, 10));
        DEPRECATION_DATES.put("dotnet8", LocalDate.of(2026, 11, 10));

        DEPRECATION_DATES.put("ruby4.0", LocalDate.of(2029, 3, 31));
        DEPRECATION_DATES.put("ruby3.4", LocalDate.of(2028, 3, 31));
        DEPRECATION_DATES.put("ruby3.3", LocalDate.of(2027, 3, 31));

        DEPRECATION_DATES.put("provided.al2023", LocalDate.of(2029, 6, 30));

        // --- Already deprecated (past end of support) ---
        DEPRECATION_DATES.put("provided.al2", LocalDate.of(2026, 7, 31));
        DEPRECATION_DATES.put("nodejs20.x", LocalDate.of(2026, 4, 30));
        DEPRECATION_DATES.put("ruby3.2", LocalDate.of(2026, 3, 31));
        DEPRECATION_DATES.put("python3.9", LocalDate.of(2025, 12, 15));
        DEPRECATION_DATES.put("nodejs18.x", LocalDate.of(2025, 9, 1));
        DEPRECATION_DATES.put("dotnet6", LocalDate.of(2024, 12, 20));
        DEPRECATION_DATES.put("python3.8", LocalDate.of(2024, 10, 14));
        DEPRECATION_DATES.put("nodejs16.x", LocalDate.of(2024, 6, 12));
        DEPRECATION_DATES.put("dotnet7", LocalDate.of(2024, 5, 14));
        DEPRECATION_DATES.put("java8", LocalDate.of(2024, 1, 8));
        DEPRECATION_DATES.put("go1.x", LocalDate.of(2024, 1, 8));
        DEPRECATION_DATES.put("provided", LocalDate.of(2024, 1, 8));
        DEPRECATION_DATES.put("ruby2.7", LocalDate.of(2023, 12, 7));
        DEPRECATION_DATES.put("nodejs14.x", LocalDate.of(2023, 12, 4));
        DEPRECATION_DATES.put("python3.7", LocalDate.of(2023, 12, 4));
        DEPRECATION_DATES.put("dotnetcore3.1", LocalDate.of(2023, 4, 3));
        DEPRECATION_DATES.put("nodejs12.x", LocalDate.of(2023, 3, 31));
        DEPRECATION_DATES.put("python3.6", LocalDate.of(2022, 7, 18));
        DEPRECATION_DATES.put("dotnet5.0", LocalDate.of(2022, 5, 10));
        DEPRECATION_DATES.put("dotnetcore2.1", LocalDate.of(2022, 1, 5));
        DEPRECATION_DATES.put("nodejs10.x", LocalDate.of(2021, 7, 30));
        DEPRECATION_DATES.put("ruby2.5", LocalDate.of(2021, 7, 30));
        DEPRECATION_DATES.put("python2.7", LocalDate.of(2021, 7, 15));
        DEPRECATION_DATES.put("nodejs8.10", LocalDate.of(2020, 3, 6));
        DEPRECATION_DATES.put("nodejs4.3", LocalDate.of(2020, 3, 5));
        DEPRECATION_DATES.put("nodejs4.3-edge", LocalDate.of(2020, 3, 5));
        DEPRECATION_DATES.put("nodejs6.10", LocalDate.of(2019, 8, 12));
        DEPRECATION_DATES.put("dotnetcore1.0", LocalDate.of(2019, 6, 27));
        DEPRECATION_DATES.put("dotnetcore2.0", LocalDate.of(2019, 5, 30));
        DEPRECATION_DATES.put("nodejs", LocalDate.of(2016, 8, 30));
    }

    private static final Set<String> KNOWN_RUNTIMES = new HashSet<>();
    static {
        KNOWN_RUNTIMES.addAll(DEPRECATION_DATES.keySet());
        KNOWN_RUNTIMES.addAll(NOT_SCHEDULED);
    }

    private RuntimeLifecycle() {
    }

    /**
     * Classifies a runtime identifier as of the given reference date. Never reads the system
     * clock itself, so callers control "now" and this stays deterministic and unit-testable.
     */
    public static Status classify(String runtimeId, LocalDate asOf) {
        if (runtimeId == null || !KNOWN_RUNTIMES.contains(runtimeId)) {
            return Status.UNKNOWN;
        }
        LocalDate deprecation = DEPRECATION_DATES.get(runtimeId);
        if (deprecation == null) {
            return Status.SUPPORTED; // explicitly "not scheduled" (e.g. preview runtimes)
        }
        if (!asOf.isBefore(deprecation)) {
            return Status.DEPRECATED;
        }
        if (!asOf.isBefore(deprecation.minusDays(NOTICE_WINDOW_DAYS))) {
            return Status.APPROACHING_EOL;
        }
        return Status.SUPPORTED;
    }

    public static Optional<LocalDate> deprecationDate(String runtimeId) {
        return Optional.ofNullable(DEPRECATION_DATES.get(runtimeId));
    }

    /**
     * Call-to-action text for the config detail view's EOL banner. Returns null for
     * SUPPORTED/UNKNOWN, since those don't get a banner.
     */
    public static String bannerMessage(String runtimeId, Status status, LocalDate asOf) {
        if (status != Status.DEPRECATED && status != Status.APPROACHING_EOL) {
            return null;
        }
        LocalDate deprecation = DEPRECATION_DATES.get(runtimeId);
        if (deprecation == null) {
            return null; // shouldn't happen for these two statuses, but stay defensive
        }
        if (status == Status.DEPRECATED) {
            return "This runtime (" + runtimeId + ") reached end of support on " + deprecation
                + ". AWS no longer patches security issues for it — upgrade to a supported runtime.";
        }
        return "This runtime (" + runtimeId + ") will reach end of support on " + deprecation
            + ". Plan to upgrade to a supported runtime before then.";
    }
}
