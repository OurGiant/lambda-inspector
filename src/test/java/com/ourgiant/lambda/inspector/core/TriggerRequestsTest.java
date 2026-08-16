package com.ourgiant.lambda.inspector.core;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lambda.model.GetPolicyRequest;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TriggerRequestsTest {

    @Test
    void buildsListEventSourceMappingsRequestForFunction() {
        ListEventSourceMappingsRequest request = TriggerRequests.listEventSourceMappings("my-function");

        assertEquals("my-function", request.functionName());
    }

    @Test
    void buildsGetPolicyRequestForFunction() {
        GetPolicyRequest request = TriggerRequests.getPolicy("my-function");

        assertEquals("my-function", request.functionName());
    }
}
