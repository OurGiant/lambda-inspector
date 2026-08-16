package com.ourgiant.lambda.inspector.core;

import com.ourgiant.lambda.inspector.model.Trigger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lambda.model.EventSourceMappingConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSourceMappingTriggersTest {

    private static EventSourceMappingConfiguration mapping(String arn, String state, Integer batchSize) {
        return EventSourceMappingConfiguration.builder()
            .eventSourceArn(arn)
            .state(state)
            .batchSize(batchSize)
            .build();
    }

    @Test
    void identifiesSqsSource() {
        List<Trigger> triggers = EventSourceMappingTriggers.from(List.of(
            mapping("arn:aws:sqs:us-east-1:123456789012:my-queue", "Enabled", 10)));

        assertEquals(1, triggers.size());
        assertEquals("SQS", triggers.get(0).type);
        assertEquals("arn:aws:sqs:us-east-1:123456789012:my-queue", triggers.get(0).source);
        assertTrue(triggers.get(0).detail.contains("Enabled"));
        assertTrue(triggers.get(0).detail.contains("10"));
    }

    @Test
    void identifiesDynamoDbStreamsSource() {
        List<Trigger> triggers = EventSourceMappingTriggers.from(List.of(
            mapping("arn:aws:dynamodb:us-east-1:123456789012:table/my-table/stream/2026-01-01T00:00:00.000", "Enabled", 100)));

        assertEquals("DynamoDB Streams", triggers.get(0).type);
    }

    @Test
    void identifiesKinesisSource() {
        List<Trigger> triggers = EventSourceMappingTriggers.from(List.of(
            mapping("arn:aws:kinesis:us-east-1:123456789012:stream/my-stream", "Enabled", 100)));

        assertEquals("Kinesis", triggers.get(0).type);
    }

    @Test
    void fallsBackToGenericLabelForUnrecognizedArn() {
        List<Trigger> triggers = EventSourceMappingTriggers.from(List.of(
            mapping("arn:aws:something-new:us-east-1:123456789012:thing/my-thing", "Enabled", null)));

        assertEquals("Event Source Mapping", triggers.get(0).type);
    }

    @Test
    void handlesNullOrEmptyList() {
        assertTrue(EventSourceMappingTriggers.from(null).isEmpty());
        assertTrue(EventSourceMappingTriggers.from(List.of()).isEmpty());
    }
}
