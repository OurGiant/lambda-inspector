package com.ourgiant.lambda.inspector.core;

import com.ourgiant.lambda.inspector.model.FunctionSummary;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lambda.model.EnvironmentResponse;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.Layer;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.model.VpcConfigResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FunctionGridModelTest {

    private static FunctionConfiguration.Builder baseConfig() {
        return FunctionConfiguration.builder()
            .functionName("my-function")
            .description("Does the thing")
            .runtime(Runtime.JAVA21)
            .handler("com.example.Handler::handleRequest")
            .memorySize(512)
            .timeout(30)
            .architecturesWithStrings("arm64")
            .lastModified("2026-08-01T12:00:00.000+0000")
            .role("arn:aws:iam::123456789012:role/my-function-role")
            .layers(Layer.builder().arn("arn:aws:lambda:us-east-1:123456789012:layer:my-layer:1").build())
            .vpcConfig(VpcConfigResponse.builder()
                .subnetIds("subnet-1")
                .securityGroupIds("sg-1")
                .build())
            .environment(EnvironmentResponse.builder()
                .variables(Map.of("STAGE", "prod"))
                .build());
    }

    @Test
    void toSummaryMapsAllFields() {
        FunctionSummary summary = FunctionGridModel.toSummary(baseConfig().build());

        assertEquals("my-function", summary.functionName);
        assertEquals("Does the thing", summary.description);
        assertEquals("java21", summary.runtime);
        assertEquals("com.example.Handler::handleRequest", summary.handler);
        assertEquals(512, summary.memorySizeMb);
        assertEquals(30, summary.timeoutSeconds);
        assertEquals("arm64", summary.architecture);
        assertEquals("2026-08-01T12:00:00.000+0000", summary.lastModified);
        assertEquals("arn:aws:iam::123456789012:role/my-function-role", summary.roleArn);
        assertEquals(List.of("arn:aws:lambda:us-east-1:123456789012:layer:my-layer:1"), summary.layerArns);
        assertEquals(List.of("subnet-1"), summary.vpcSubnetIds);
        assertEquals(List.of("sg-1"), summary.vpcSecurityGroupIds);
        assertEquals(Map.of("STAGE", "prod"), summary.environmentVariables);
    }

    @Test
    void toSummaryHandlesAbsentLayersVpcAndEnvironment() {
        FunctionConfiguration config = FunctionConfiguration.builder()
            .functionName("bare-function")
            .runtime(Runtime.PYTHON3_13)
            .build();

        FunctionSummary summary = FunctionGridModel.toSummary(config);

        assertEquals(List.of(), summary.layerArns);
        assertEquals(List.of(), summary.vpcSubnetIds);
        assertEquals(List.of(), summary.vpcSecurityGroupIds);
        assertEquals(Map.of(), summary.environmentVariables);
    }

    @Test
    void toSummariesMapsEachConfigInOrder() {
        List<FunctionConfiguration> configs = List.of(
            baseConfig().functionName("fn-a").build(),
            baseConfig().functionName("fn-b").build());

        List<FunctionSummary> summaries = FunctionGridModel.toSummaries(configs);

        assertEquals(2, summaries.size());
        assertEquals("fn-a", summaries.get(0).functionName);
        assertEquals("fn-b", summaries.get(1).functionName);
    }

    @Test
    void formatRowSubstitutesPlaceholderForMissingStrings() {
        FunctionSummary summary = new FunctionSummary(
            null, null, null, null, null, null, null, null, null, List.of(), List.of(), List.of(), Map.of());

        List<Object> row = FunctionGridModel.formatRow(summary);

        assertEquals(List.of("—", "—", "—"), List.of(row.get(0), row.get(1), row.get(4)));
        assertNull(row.get(2));
        assertNull(row.get(3));
    }

    @Test
    void formatRowKeepsRealValues() {
        FunctionSummary summary = FunctionGridModel.toSummary(baseConfig().build());

        List<Object> row = FunctionGridModel.formatRow(summary);

        assertEquals(List.of("my-function", "java21", 512, 30, "2026-08-01T12:00:00.000+0000"), row);
    }
}
