package com.ourgiant.lambda.inspector.model;

// One thing that can invoke a function - either a poll-based event source mapping (SQS,
// DynamoDB Streams, Kinesis, ...) or a push-based resource-policy grant (S3, API Gateway,
// EventBridge, SNS, ...). See core.EventSourceMappingTriggers and core.ResourcePolicyTriggers,
// which map AWS's two entirely different API shapes for "what triggers this function" into this
// one type so the GUI (gui.FunctionDetailDialog) doesn't need to know which kind it's showing.
public class Trigger {
    public final String type;
    public final String source;
    public final String detail;

    public Trigger(String type, String source, String detail) {
        this.type = type;
        this.source = source;
        this.detail = detail;
    }
}
