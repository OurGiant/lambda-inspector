package com.ourgiant.lambda.inspector.core;

import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest;

// Pure request builder for paginated ListFunctions calls, kept separate from the
// LambdaClient call site (see gui.MainWindow) the same way dynamodb-client's QueryRequests
// keeps QueryRequest building out of DynamoDBBrowserFrame.
public final class LambdaListRequests {

    private LambdaListRequests() {
    }

    public static ListFunctionsRequest build(int maxItems, String marker) {
        ListFunctionsRequest.Builder builder = ListFunctionsRequest.builder()
            .maxItems(maxItems);
        if (marker != null && !marker.isEmpty()) {
            builder.marker(marker);
        }
        return builder.build();
    }
}
