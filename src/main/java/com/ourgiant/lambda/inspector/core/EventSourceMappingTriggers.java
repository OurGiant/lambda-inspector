package com.ourgiant.lambda.inspector.core;

import com.ourgiant.lambda.inspector.model.Trigger;
import software.amazon.awssdk.services.lambda.model.EventSourceMappingConfiguration;

import java.util.ArrayList;
import java.util.List;

// Pure mapping from ListEventSourceMappings results to the shared Trigger model - poll-based
// triggers (SQS, DynamoDB Streams, Kinesis, MSK, Amazon MQ, DocumentDB), identified by pattern-
// matching the event source's own ARN since EventSourceMappingConfiguration doesn't carry a
// separate "service" field.
public final class EventSourceMappingTriggers {

    private EventSourceMappingTriggers() {
    }

    public static List<Trigger> from(List<EventSourceMappingConfiguration> mappings) {
        List<Trigger> triggers = new ArrayList<>();
        if (mappings == null) {
            return triggers;
        }
        for (EventSourceMappingConfiguration mapping : mappings) {
            String type = friendlyServiceFromArn(mapping.eventSourceArn());
            String state = mapping.state();
            String detail = state != null
                ? "State: " + state + (mapping.batchSize() != null ? ", batch size: " + mapping.batchSize() : "")
                : (mapping.batchSize() != null ? "Batch size: " + mapping.batchSize() : null);
            triggers.add(new Trigger(type, orDash(mapping.eventSourceArn()), detail));
        }
        return triggers;
    }

    private static String friendlyServiceFromArn(String arn) {
        if (arn == null) {
            return "Event Source Mapping";
        }
        if (arn.contains(":sqs:")) {
            return "SQS";
        }
        if (arn.contains(":dynamodb:")) {
            return "DynamoDB Streams";
        }
        if (arn.contains(":kinesis:")) {
            return "Kinesis";
        }
        if (arn.contains(":kafka:") || arn.contains(":mskcluster")) {
            return "MSK (Kafka)";
        }
        if (arn.contains(":mq:")) {
            return "Amazon MQ";
        }
        if (arn.contains(":docdb")) {
            return "DocumentDB";
        }
        return "Event Source Mapping";
    }

    private static String orDash(String value) {
        return value != null && !value.isBlank() ? value : "—";
    }
}
