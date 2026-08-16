package com.ourgiant.lambda.inspector.core;

import com.fasterxml.jackson.databind.ObjectMapper;

// Pretty-prints an invoke response payload when it's JSON (the common case for Lambda), and
// falls back to the raw text otherwise, so the invoke dialog (gui.InvokeDialog) never has to
// guess which case it's in.
public final class ResponseFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ResponseFormatter() {
    }

    public static String prettyPrintOrRaw(String raw) {
        if (raw == null) {
            return "";
        }
        try {
            Object parsed = MAPPER.readValue(raw, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            return raw;
        }
    }
}
