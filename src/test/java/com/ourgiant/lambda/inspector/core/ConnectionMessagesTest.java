package com.ourgiant.lambda.inspector.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectionMessagesTest {

    @Test
    void plainTitleWhenNoProfileConnected() {
        assertEquals("Lambda Inspector", ConnectionMessages.windowTitle(null, null, null));
    }

    @Test
    void includesProfileAccountAndRegionWhenKnown() {
        assertEquals("Lambda Inspector — dev (123456789012, us-east-1)",
            ConnectionMessages.windowTitle("dev", "123456789012", "us-east-1"));
    }

    @Test
    void degradesGracefullyWithoutAccountId() {
        assertEquals("Lambda Inspector — dev (us-east-1)",
            ConnectionMessages.windowTitle("dev", null, "us-east-1"));
    }
}
