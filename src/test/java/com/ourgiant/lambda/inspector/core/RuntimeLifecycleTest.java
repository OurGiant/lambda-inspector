package com.ourgiant.lambda.inspector.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeLifecycleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @ParameterizedTest
    @ValueSource(strings = {"python3.9", "nodejs18.x", "go1.x", "java8", "provided"})
    void classifiesKnownPastDeprecationDatesAsDeprecated(String runtimeId) {
        assertEquals(RuntimeLifecycle.Status.DEPRECATED, RuntimeLifecycle.classify(runtimeId, TODAY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"python3.13", "nodejs24.x", "java21", "ruby4.0"})
    void classifiesFarFutureDeprecationsAsSupported(String runtimeId) {
        assertEquals(RuntimeLifecycle.Status.SUPPORTED, RuntimeLifecycle.classify(runtimeId, TODAY));
    }

    @Test
    void classifiesPreviewRuntimesWithNoScheduledDateAsSupported() {
        assertEquals(RuntimeLifecycle.Status.SUPPORTED, RuntimeLifecycle.classify("nodejs26.x", TODAY));
        assertEquals(RuntimeLifecycle.Status.SUPPORTED, RuntimeLifecycle.classify("python3.15", TODAY));
    }

    @Test
    void classifiesRuntimeWithinNoticeWindowAsApproachingEol() {
        // python3.10 deprecates 2026-10-31; 2026-08-15 is within the 180-day notice window
        // (2026-05-04 onward) but before the deprecation date itself.
        assertEquals(RuntimeLifecycle.Status.APPROACHING_EOL,
            RuntimeLifecycle.classify("python3.10", TODAY));
    }

    @Test
    void classifiesRuntimeJustOutsideNoticeWindowAsSupported() {
        LocalDate justOutsideWindow = LocalDate.of(2026, 10, 31).minusDays(RuntimeLifecycle.NOTICE_WINDOW_DAYS + 1);
        assertEquals(RuntimeLifecycle.Status.SUPPORTED,
            RuntimeLifecycle.classify("python3.10", justOutsideWindow));
    }

    @Test
    void classifiesRuntimeOnItsDeprecationDateAsDeprecated() {
        assertEquals(RuntimeLifecycle.Status.DEPRECATED,
            RuntimeLifecycle.classify("python3.10", LocalDate.of(2026, 10, 31)));
    }

    @Test
    void classifiesUnknownIdentifierAsUnknown() {
        assertEquals(RuntimeLifecycle.Status.UNKNOWN, RuntimeLifecycle.classify("cobol9000", TODAY));
    }

    @Test
    void classifiesNullRuntimeAsUnknown() {
        // FunctionConfiguration.runtimeAsString() is null for container-image functions.
        assertEquals(RuntimeLifecycle.Status.UNKNOWN, RuntimeLifecycle.classify(null, TODAY));
    }

    @Test
    void distinguishesJava8VariantsByOperatingSystem() {
        assertEquals(RuntimeLifecycle.Status.DEPRECATED, RuntimeLifecycle.classify("java8", TODAY));
        assertEquals(RuntimeLifecycle.Status.SUPPORTED, RuntimeLifecycle.classify("java8.al2", TODAY));
        assertEquals(RuntimeLifecycle.Status.SUPPORTED, RuntimeLifecycle.classify("java8.al2023", TODAY));
    }

    @Test
    void distinguishesProvidedVariantsByOperatingSystem() {
        assertEquals(RuntimeLifecycle.Status.DEPRECATED, RuntimeLifecycle.classify("provided", TODAY));
        assertEquals(RuntimeLifecycle.Status.DEPRECATED, RuntimeLifecycle.classify("provided.al2", TODAY));
        assertEquals(RuntimeLifecycle.Status.SUPPORTED, RuntimeLifecycle.classify("provided.al2023", TODAY));
    }

    @Test
    void bannerMessageIsNullForSupportedAndUnknown() {
        assertNull(RuntimeLifecycle.bannerMessage("python3.13", RuntimeLifecycle.Status.SUPPORTED, TODAY));
        assertNull(RuntimeLifecycle.bannerMessage("cobol9000", RuntimeLifecycle.Status.UNKNOWN, TODAY));
    }

    @Test
    void bannerMessageMentionsDateForDeprecatedAndApproachingEol() {
        String deprecated = RuntimeLifecycle.bannerMessage("python3.9", RuntimeLifecycle.Status.DEPRECATED, TODAY);
        assertTrue(deprecated.contains("python3.9"));
        assertTrue(deprecated.contains("2025-12-15"));
        assertTrue(deprecated.contains("reached end of support"));

        String approaching = RuntimeLifecycle.bannerMessage(
            "python3.10", RuntimeLifecycle.Status.APPROACHING_EOL, TODAY);
        assertTrue(approaching.contains("python3.10"));
        assertTrue(approaching.contains("2026-10-31"));
        assertTrue(approaching.contains("will reach end of support"));
    }

    @Test
    void deprecationDateIsEmptyForUnknownOrNotScheduledRuntimes() {
        assertTrue(RuntimeLifecycle.deprecationDate("cobol9000").isEmpty());
        assertTrue(RuntimeLifecycle.deprecationDate("nodejs26.x").isEmpty());
        assertEquals(LocalDate.of(2026, 10, 31), RuntimeLifecycle.deprecationDate("python3.10").get());
    }
}
