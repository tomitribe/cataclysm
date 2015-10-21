/*
 * Tomitribe Confidential
 *
 * Copyright Tomitribe Corporation. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.tomitribe.cataclysm;

import org.apache.http.HttpResponse;

public class Response {

    private final HttpResponse httpResponse;
    private final String content;

    public Response(HttpResponse httpResponse, String content) {
        this.httpResponse = httpResponse;
        this.content = content;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public String getContent() {
        return content;
    }
}
