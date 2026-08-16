package com.ourgiant.lambda.inspector.core;

import com.ourgiant.lambda.inspector.model.FunctionSummary;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.Layer;
import software.amazon.awssdk.services.lambda.model.VpcConfigResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Pure mapping/formatting logic behind the functions grid: which columns to show and how to
// turn an SDK FunctionConfiguration into a row, kept separate from the JTable/DefaultTableModel
// that actually displays it (see gui.MainWindow), the same split dynamodb-client's
// RecordGridModel makes for its records grid. Also maps the fields the config detail view needs
// (gui.FunctionDetailDialog) - ListFunctions' FunctionConfiguration already carries all of it
// (environment, vpcConfig, layers, ...), so no separate GetFunction/GetFunctionConfiguration
// call is needed.
public final class FunctionGridModel {

    public static final List<String> COLUMN_NAMES = List.of(
        "Function Name", "Runtime", "Memory (MB)", "Timeout (s)", "Last Modified");

    private static final String UNKNOWN = "—";

    private FunctionGridModel() {
    }

    public static FunctionSummary toSummary(FunctionConfiguration config) {
        List<String> layerArns = new ArrayList<>();
        if (config.hasLayers()) {
            for (Layer layer : config.layers()) {
                layerArns.add(layer.arn());
            }
        }

        List<String> subnetIds = List.of();
        List<String> securityGroupIds = List.of();
        VpcConfigResponse vpc = config.vpcConfig();
        if (vpc != null) {
            if (vpc.hasSubnetIds()) {
                subnetIds = vpc.subnetIds();
            }
            if (vpc.hasSecurityGroupIds()) {
                securityGroupIds = vpc.securityGroupIds();
            }
        }

        Map<String, String> envVars = new LinkedHashMap<>();
        if (config.environment() != null && config.environment().hasVariables()) {
            envVars.putAll(config.environment().variables());
        }

        String architecture = config.hasArchitectures() && !config.architecturesAsStrings().isEmpty()
            ? config.architecturesAsStrings().get(0)
            : null;

        return new FunctionSummary(
            config.functionName(),
            config.description(),
            config.runtimeAsString(),
            config.handler(),
            config.memorySize(),
            config.timeout(),
            architecture,
            config.lastModified(),
            config.role(),
            layerArns,
            subnetIds,
            securityGroupIds,
            Collections.unmodifiableMap(envVars));
    }

    public static List<FunctionSummary> toSummaries(List<FunctionConfiguration> configs) {
        List<FunctionSummary> summaries = new ArrayList<>();
        for (FunctionConfiguration config : configs) {
            summaries.add(toSummary(config));
        }
        return summaries;
    }

    public static List<Object> formatRow(FunctionSummary summary) {
        List<Object> row = new ArrayList<>();
        row.add(summary.functionName != null ? summary.functionName : UNKNOWN);
        row.add(summary.runtime != null ? summary.runtime : UNKNOWN);
        row.add(summary.memorySizeMb);
        row.add(summary.timeoutSeconds);
        row.add(summary.lastModified != null ? summary.lastModified : UNKNOWN);
        return row;
    }
}
