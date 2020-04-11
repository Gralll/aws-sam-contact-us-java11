package com.gralll.sam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gralll.sam.exception.ContactUsLambdaServerException;
import com.gralll.sam.model.ContactUsResponseBody;
import com.gralll.sam.model.ContactUsProxyResponse;
import org.apache.http.entity.ContentType;

import java.util.HashMap;
import java.util.Map;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;

public class ResponseService {

    private final ObjectMapper objectMapper;

    public ResponseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ContactUsProxyResponse buildWarmUpResponse() {
        return buildResponse(201, "Lambda was warmed up. V2");
    }

    public ContactUsProxyResponse buildResponse(int statusCode, String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        headers.put("Access-Control-Allow-Origin", "*");
        return new ContactUsProxyResponse(statusCode, headers, getBodyAsString(body));
    }

    private String getBodyAsString(String body) {
        try {
            return objectMapper.writeValueAsString(new ContactUsResponseBody(body));
        } catch (JsonProcessingException e) {
            throw new ContactUsLambdaServerException("Writing ContactUsResponseBody as string failed.", e);
        }
    }
}

