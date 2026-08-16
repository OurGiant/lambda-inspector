package com.ourgiant.lambda.inspector.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvokeLogParserTest {

    private static final String COLD_START_LOG =
        "START RequestId: 8f2a1b3c-1111-2222-3333-444455556666 Version: $LATEST\n"
            + "END RequestId: 8f2a1b3c-1111-2222-3333-444455556666\n"
            + "REPORT RequestId: 8f2a1b3c-1111-2222-3333-444455556666\tDuration: 12.34 ms\t"
            + "Billed Duration: 13 ms\tMemory Size: 256 MB\tMax Memory Used: 45 MB\t"
            + "Init Duration: 150.23 ms\n";

    private static final String WARM_START_LOG =
        "START RequestId: 9a1b2c3d-1111-2222-3333-444455556666 Version: $LATEST\n"
            + "END RequestId: 9a1b2c3d-1111-2222-3333-444455556666\n"
            + "REPORT RequestId: 9a1b2c3d-1111-2222-3333-444455556666\tDuration: 5.67 ms\t"
            + "Billed Duration: 6 ms\tMemory Size: 512 MB\tMax Memory Used: 60 MB\n";

    @Test
    void parsesColdStartReportWithInitDuration() {
        Optional<InvokeLogParser.ExecutionReport> result = InvokeLogParser.parse(COLD_START_LOG);

        assertTrue(result.isPresent());
        InvokeLogParser.ExecutionReport report = result.get();
        assertTrue(report.isColdStart());
        assertEquals(12.34, report.durationMs());
        assertEquals(13, report.billedDurationMs());
        assertEquals(256, report.memorySizeMb());
        assertEquals(45, report.maxMemoryUsedMb());
        assertEquals(150.23, report.initDurationMs());
    }

    @Test
    void parsesWarmStartReportWithoutInitDuration() {
        Optional<InvokeLogParser.ExecutionReport> result = InvokeLogParser.parse(WARM_START_LOG);

        assertTrue(result.isPresent());
        InvokeLogParser.ExecutionReport report = result.get();
        assertFalse(report.isColdStart());
        assertEquals(5.67, report.durationMs());
        assertEquals(6, report.billedDurationMs());
        assertEquals(512, report.memorySizeMb());
        assertEquals(60, report.maxMemoryUsedMb());
        assertEquals(null, report.initDurationMs());
    }

    @Test
    void returnsEmptyForMissingOrGarbageReportLine() {
        assertTrue(InvokeLogParser.parse(null).isEmpty());
        assertTrue(InvokeLogParser.parse("").isEmpty());
        assertTrue(InvokeLogParser.parse("some unrelated log output\nwith no REPORT line").isEmpty());
    }

    @Test
    void parseBase64DecodesAndParses() {
        String encoded = Base64.getEncoder().encodeToString(COLD_START_LOG.getBytes(StandardCharsets.UTF_8));

        Optional<InvokeLogParser.ExecutionReport> result = InvokeLogParser.parseBase64(encoded);

        assertTrue(result.isPresent());
        assertTrue(result.get().isColdStart());
    }

    @Test
    void parseBase64ReturnsEmptyForNullBlankOrInvalidInput() {
        assertTrue(InvokeLogParser.parseBase64(null).isEmpty());
        assertTrue(InvokeLogParser.parseBase64("").isEmpty());
        assertTrue(InvokeLogParser.parseBase64("not-valid-base64!!!").isEmpty());
    }
}
