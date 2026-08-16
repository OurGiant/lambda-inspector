package com.ourgiant.lambda.inspector.model;

import java.util.List;
import java.util.Map;

// One function's worth of detail, fetched in a single ListFunctions call (FunctionConfiguration
// already carries everything below - see core.FunctionGridModel.toSummary - so no separate
// GetFunction/GetFunctionConfiguration call is needed for the config detail view). The functions
// grid (core.FunctionGridModel) only displays a handful of these columns; the rest are shown in
// the config detail view (gui.FunctionDetailDialog).
public class FunctionSummary {
    public final String functionName;
    public final String description;
    public final String runtime;
    public final String handler;
    public final Integer memorySizeMb;
    public final Integer timeoutSeconds;
    public final String architecture;
    public final String lastModified;
    public final String roleArn;
    public final List<String> layerArns;
    public final List<String> vpcSubnetIds;
    public final List<String> vpcSecurityGroupIds;
    public final Map<String, String> environmentVariables;

    public FunctionSummary(String functionName, String description, String runtime, String handler,
            Integer memorySizeMb, Integer timeoutSeconds, String architecture, String lastModified,
            String roleArn, List<String> layerArns, List<String> vpcSubnetIds,
            List<String> vpcSecurityGroupIds, Map<String, String> environmentVariables) {
        this.functionName = functionName;
        this.description = description;
        this.runtime = runtime;
        this.handler = handler;
        this.memorySizeMb = memorySizeMb;
        this.timeoutSeconds = timeoutSeconds;
        this.architecture = architecture;
        this.lastModified = lastModified;
        this.roleArn = roleArn;
        this.layerArns = layerArns;
        this.vpcSubnetIds = vpcSubnetIds;
        this.vpcSecurityGroupIds = vpcSecurityGroupIds;
        this.environmentVariables = environmentVariables;
    }
}
