package com.ourgiant.lambda.inspector.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourgiant.lambda.inspector.model.Trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Pure parsing of a function's resource-based policy (GetPolicy's `policy` field - a JSON IAM
// policy document, standard shape: {"Statement": [{"Sid", "Principal": {"Service": "..."} or
// {"AWS": "..."}, "Condition": {"ArnLike": {"AWS:SourceArn": "..."}}}]}) into the shared Trigger
// model - push-based triggers (S3, API Gateway, EventBridge, SNS, ...), each added to the
// function's policy via AddPermission when the trigger was wired up in the AWS console/CLI.
public final class ResourcePolicyTriggers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Not exhaustive - covers the common Lambda trigger sources. An unmapped principal falls
    // back to showing the raw service string, which is still useful, just not prettified.
    private static final Map<String, String> FRIENDLY_SERVICE_NAMES = Map.ofEntries(
        Map.entry("s3.amazonaws.com", "S3"),
        Map.entry("sns.amazonaws.com", "SNS"),
        Map.entry("events.amazonaws.com", "EventBridge"),
        Map.entry("apigateway.amazonaws.com", "API Gateway"),
        Map.entry("elasticloadbalancing.amazonaws.com", "Application Load Balancer"),
        Map.entry("cloudfront.amazonaws.com", "CloudFront (Lambda@Edge)"),
        Map.entry("iot.amazonaws.com", "IoT Core"),
        Map.entry("cognito-idp.amazonaws.com", "Cognito"),
        Map.entry("logs.amazonaws.com", "CloudWatch Logs subscription"));

    private ResourcePolicyTriggers() {
    }

    /** Returns an empty list for a null/blank/malformed policy, rather than throwing. */
    public static List<Trigger> parse(String policyJson) {
        List<Trigger> triggers = new ArrayList<>();
        if (policyJson == null || policyJson.isBlank()) {
            return triggers;
        }
        try {
            JsonNode root = MAPPER.readTree(policyJson);
            JsonNode statements = root.path("Statement");
            if (statements.isArray()) {
                for (JsonNode statement : statements) {
                    triggers.add(fromStatement(statement));
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return triggers;
    }

    private static Trigger fromStatement(JsonNode statement) {
        JsonNode principal = statement.path("Principal");
        String type;
        if (principal.has("Service")) {
            String service = principal.path("Service").asText();
            type = FRIENDLY_SERVICE_NAMES.getOrDefault(service, service);
        } else if (principal.has("AWS") || principal.isTextual()) {
            type = "Direct invoke grant";
        } else {
            type = "Unknown principal";
        }

        String sourceArn = statement.path("Condition").path("ArnLike").path("AWS:SourceArn").asText(null);
        if (sourceArn == null) {
            sourceArn = statement.path("Condition").path("ArnEquals").path("AWS:SourceArn").asText(null);
        }
        String source = sourceArn != null ? sourceArn : "(not restricted to a specific source ARN)";

        String sid = statement.path("Sid").asText(null);
        return new Trigger(type, source, sid);
    }
}
