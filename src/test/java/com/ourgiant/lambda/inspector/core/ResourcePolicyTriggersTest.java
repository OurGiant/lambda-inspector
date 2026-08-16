package com.ourgiant.lambda.inspector.core;

import com.ourgiant.lambda.inspector.model.Trigger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePolicyTriggersTest {

    @Test
    void parsesS3TriggerWithSourceArn() {
        String policy = """
            {
              "Version": "2012-10-17",
              "Id": "default",
              "Statement": [
                {
                  "Sid": "s3-trigger",
                  "Effect": "Allow",
                  "Principal": {"Service": "s3.amazonaws.com"},
                  "Action": "lambda:InvokeFunction",
                  "Resource": "arn:aws:lambda:us-east-1:123456789012:function:my-function",
                  "Condition": {"ArnLike": {"AWS:SourceArn": "arn:aws:s3:::my-bucket"}}
                }
              ]
            }
            """;

        List<Trigger> triggers = ResourcePolicyTriggers.parse(policy);

        assertEquals(1, triggers.size());
        assertEquals("S3", triggers.get(0).type);
        assertEquals("arn:aws:s3:::my-bucket", triggers.get(0).source);
        assertEquals("s3-trigger", triggers.get(0).detail);
    }

    @Test
    void parsesApiGatewayTrigger() {
        String policy = """
            {"Statement": [{"Sid": "apigw", "Principal": {"Service": "apigateway.amazonaws.com"},
              "Condition": {"ArnLike": {"AWS:SourceArn": "arn:aws:execute-api:us-east-1:123456789012:abc123/*/GET/hello"}}}]}
            """;

        List<Trigger> triggers = ResourcePolicyTriggers.parse(policy);

        assertEquals("API Gateway", triggers.get(0).type);
        assertTrue(triggers.get(0).source.contains("execute-api"));
    }

    @Test
    void parsesEventBridgeTrigger() {
        String policy = """
            {"Statement": [{"Sid": "eventbridge-rule", "Principal": {"Service": "events.amazonaws.com"},
              "Condition": {"ArnLike": {"AWS:SourceArn": "arn:aws:events:us-east-1:123456789012:rule/my-rule"}}}]}
            """;

        List<Trigger> triggers = ResourcePolicyTriggers.parse(policy);

        assertEquals("EventBridge", triggers.get(0).type);
    }

    @Test
    void treatsAwsPrincipalAsDirectInvokeGrant() {
        String policy = """
            {"Statement": [{"Sid": "cross-account", "Principal": {"AWS": "arn:aws:iam::999999999999:root"}}]}
            """;

        List<Trigger> triggers = ResourcePolicyTriggers.parse(policy);

        assertEquals("Direct invoke grant", triggers.get(0).type);
    }

    @Test
    void fallsBackToRawServiceStringForUnmappedService() {
        String policy = """
            {"Statement": [{"Sid": "future-service", "Principal": {"Service": "some-future-service.amazonaws.com"}}]}
            """;

        List<Trigger> triggers = ResourcePolicyTriggers.parse(policy);

        assertEquals("some-future-service.amazonaws.com", triggers.get(0).type);
    }

    @Test
    void showsPlaceholderWhenNoSourceArnCondition() {
        String policy = """
            {"Statement": [{"Sid": "no-condition", "Principal": {"Service": "s3.amazonaws.com"}}]}
            """;

        List<Trigger> triggers = ResourcePolicyTriggers.parse(policy);

        assertEquals("(not restricted to a specific source ARN)", triggers.get(0).source);
    }

    @Test
    void returnsEmptyListForNullBlankOrMalformedPolicy() {
        assertTrue(ResourcePolicyTriggers.parse(null).isEmpty());
        assertTrue(ResourcePolicyTriggers.parse("").isEmpty());
        assertTrue(ResourcePolicyTriggers.parse("not json at all").isEmpty());
    }

    @Test
    void handlesMultipleStatements() {
        String policy = """
            {"Statement": [
              {"Sid": "s3-trigger", "Principal": {"Service": "s3.amazonaws.com"},
               "Condition": {"ArnLike": {"AWS:SourceArn": "arn:aws:s3:::bucket-a"}}},
              {"Sid": "sns-trigger", "Principal": {"Service": "sns.amazonaws.com"},
               "Condition": {"ArnLike": {"AWS:SourceArn": "arn:aws:sns:us-east-1:123456789012:my-topic"}}}
            ]}
            """;

        List<Trigger> triggers = ResourcePolicyTriggers.parse(policy);

        assertEquals(2, triggers.size());
        assertEquals("S3", triggers.get(0).type);
        assertEquals("SNS", triggers.get(1).type);
    }
}
