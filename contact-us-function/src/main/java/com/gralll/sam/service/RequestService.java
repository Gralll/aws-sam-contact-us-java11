package com.gralll.sam.service;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gralll.sam.exception.ContactUsLambdaClientException;
import com.gralll.sam.model.ContactUsRequest;

import java.io.IOException;

public class RequestService {

    private final ObjectMapper objectMapper;

    public RequestService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ContactUsRequest getContactUsRequest(AwsProxyRequest request) {
        try {
            return objectMapper.readValue(request.getBody(), ContactUsRequest.class);
        } catch (IOException e) {
            throw new ContactUsLambdaClientException("ContactUs request deserialization failed.", e);
        }
    }

    public String getAsPrettyString(AwsProxyRequest request) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new ContactUsLambdaClientException("Writing AwsProxyRequest as String failed.", e);
        }
    }

}
