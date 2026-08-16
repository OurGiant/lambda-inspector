package com.ourgiant.lambda.inspector.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

// Validates a Lambda invoke payload is well-formed JSON before the invoke dialog (gui.InvokeDialog)
// makes the actual AWS call, so a typo shows up immediately rather than as an opaque SDK error.
public final class PayloadValidation {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PayloadValidation() {
    }

    /** Empty if {@code json} is valid JSON; otherwise the parse error message. */
    public static Optional<String> validate(String json) {
        if (json == null || json.isBlank()) {
            return Optional.of("Payload is empty.");
        }
        try {
            MAPPER.readTree(json);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            return Optional.of(e.getOriginalMessage());
        }
    }
}
