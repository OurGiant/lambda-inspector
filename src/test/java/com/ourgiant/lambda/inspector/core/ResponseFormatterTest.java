package com.ourgiant.lambda.inspector.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseFormatterTest {

    @Test
    void prettyPrintsValidJson() {
        String result = ResponseFormatter.prettyPrintOrRaw("{\"statusCode\":200,\"body\":\"ok\"}");

        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\"statusCode\""));
        assertTrue(result.contains("200"));
    }

    @Test
    void returnsNonJsonStringUnchanged() {
        assertEquals("plain text response", ResponseFormatter.prettyPrintOrRaw("plain text response"));
    }

    @Test
    void returnsEmptyStringForNull() {
        assertEquals("", ResponseFormatter.prettyPrintOrRaw(null));
    }
}
