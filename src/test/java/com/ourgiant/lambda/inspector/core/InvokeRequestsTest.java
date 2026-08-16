package com.ourgiant.lambda.inspector.core;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.LogType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvokeRequestsTest {

    @Test
    void buildsRequestWithGivenFunctionNameAndPayload() {
        InvokeRequest request = InvokeRequests.build("my-function", "{\"key\":\"value\"}");

        assertEquals("my-function", request.functionName());
        assertEquals(InvocationType.REQUEST_RESPONSE, request.invocationType());
        assertEquals(LogType.TAIL, request.logType());
        assertEquals("{\"key\":\"value\"}", request.payload().asUtf8String());
    }

    @Test
    void defaultsToEmptyJsonObjectForNullPayload() {
        InvokeRequest request = InvokeRequests.build("my-function", null);

        assertEquals("{}", request.payload().asUtf8String());
    }

    @Test
    void defaultsToEmptyJsonObjectForBlankPayload() {
        InvokeRequest request = InvokeRequests.build("my-function", "   ");

        assertEquals("{}", request.payload().asUtf8String());
    }
}
