package com.ourgiant.lambda.inspector.core;

import software.amazon.awssdk.services.lambda.model.GetPolicyRequest;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsRequest;

// Pure request-building logic behind the triggers section of the config detail view
// (see gui.FunctionDetailDialog) - the two AWS calls needed to answer "what triggers this
// function": ListEventSourceMappings for poll-based sources (SQS, DynamoDB Streams, Kinesis, ...)
// and GetPolicy for push-based ones, which show up as resource-policy grants (S3, API Gateway,
// EventBridge, SNS, ...) rather than event source mappings.
public final class TriggerRequests {

    private TriggerRequests() {
    }

    public static ListEventSourceMappingsRequest listEventSourceMappings(String functionName) {
        return ListEventSourceMappingsRequest.builder()
            .functionName(functionName)
            .build();
    }

    public static GetPolicyRequest getPolicy(String functionName) {
        return GetPolicyRequest.builder()
            .functionName(functionName)
            .build();
    }
}
