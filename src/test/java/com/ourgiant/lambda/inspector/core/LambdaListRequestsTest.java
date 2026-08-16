package com.ourgiant.lambda.inspector.core;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LambdaListRequestsTest {

    @Test
    void omitsMarkerOnFirstPage() {
        ListFunctionsRequest request = LambdaListRequests.build(50, null);

        assertEquals(50, request.maxItems());
        assertNull(request.marker());
    }

    @Test
    void omitsMarkerWhenEmpty() {
        ListFunctionsRequest request = LambdaListRequests.build(50, "");

        assertNull(request.marker());
    }

    @Test
    void includesMarkerForSubsequentPages() {
        ListFunctionsRequest request = LambdaListRequests.build(50, "page-2-marker");

        assertEquals("page-2-marker", request.marker());
    }
}
