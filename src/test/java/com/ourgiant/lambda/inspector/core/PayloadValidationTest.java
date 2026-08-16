package com.ourgiant.lambda.inspector.core;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadValidationTest {

    @Test
    void validJsonObjectIsValid() {
        assertTrue(PayloadValidation.validate("{\"key\": \"value\"}").isEmpty());
    }

    @Test
    void validJsonArrayIsValid() {
        assertTrue(PayloadValidation.validate("[1, 2, 3]").isEmpty());
    }

    @Test
    void trailingCommaIsInvalid() {
        Optional<String> error = PayloadValidation.validate("{\"key\": \"value\",}");
        assertTrue(error.isPresent());
    }

    @Test
    void unclosedBraceIsInvalid() {
        Optional<String> error = PayloadValidation.validate("{\"key\": \"value\"");
        assertTrue(error.isPresent());
    }

    @Test
    void nullOrBlankIsInvalid() {
        assertTrue(PayloadValidation.validate(null).isPresent());
        assertTrue(PayloadValidation.validate("").isPresent());
        assertTrue(PayloadValidation.validate("   ").isPresent());
    }
}
