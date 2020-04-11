package com.gralll.sam.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Just a copy of aws-sdk response, but with a fixed 'is' field
 */
public class ContactUsProxyResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;
    private final boolean isBase64Encoded;

    public ContactUsProxyResponse(int statusCode, Map<String, String> headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.isBase64Encoded = false;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    @JsonProperty("isBase64Encoded")
    public boolean getIsBase64Encoded() {
        return isBase64Encoded;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ContactUsProxyResponse that = (ContactUsProxyResponse) o;
        return statusCode == that.statusCode &&
                isBase64Encoded == that.isBase64Encoded &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(body, that.body);
    }

    @Override public int hashCode() {
        return Objects.hash(statusCode, headers, body, isBase64Encoded);
    }
}
