package com.ourgiant.lambda.inspector.core;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.LogType;

// Pure request-building logic behind the invoke feature (see gui.InvokeDialog), kept separate
// from the SwingWorker/JTextArea wiring that actually fires it, the same split
// core.LambdaListRequests makes for the ListFunctions request.
public final class InvokeRequests {

    private InvokeRequests() {
    }

    /**
     * Builds a synchronous ("RequestResponse") invoke request. Always requests LogType.TAIL so
     * the response carries the execution log's REPORT line (see InvokeLogParser), which is how
     * duration/billed-duration/cold-start get surfaced in the UI without a second API call.
     */
    public static InvokeRequest build(String functionName, String payloadJson) {
        String payload = payloadJson != null && !payloadJson.isBlank() ? payloadJson : "{}";
        return InvokeRequest.builder()
            .functionName(functionName)
            .invocationType(InvocationType.REQUEST_RESPONSE)
            .logType(LogType.TAIL)
            .payload(SdkBytes.fromUtf8String(payload))
            .build();
    }
}
