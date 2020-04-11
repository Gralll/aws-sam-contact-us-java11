package com.gralll.sam;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gralll.sam.exception.ContactUsLambdaClientException;
import com.gralll.sam.model.ContactUsProxyResponse;
import com.gralll.sam.service.AwsClientFactory;
import com.gralll.sam.service.DbService;
import com.gralll.sam.service.EmailService;
import com.gralll.sam.service.RequestService;
import com.gralll.sam.service.ResponseService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest(App.class)
public class AppTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private App app;

    @Mock
    private EmailService emailService;
    @Mock
    private DbService dbService;
    @Mock
    private RequestService requestService;
    @Mock
    private ResponseService responseService;
    @Mock
    private Context context;
    @Mock
    private AwsClientFactory awsClientFactory;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        whenNew(AwsClientFactory.class).withNoArguments().thenReturn(awsClientFactory);
        whenNew(EmailService.class).withArguments(any()).thenReturn(emailService);
        whenNew(DbService.class).withArguments(any()).thenReturn(dbService);
        whenNew(ResponseService.class).withArguments(any()).thenReturn(responseService);
        whenNew(RequestService.class).withArguments(any()).thenReturn(requestService);
        app = new App();
    }

    @Test
    public void shouldReturnSuccessfulResponse() throws IOException {
        // given
        ContactUsProxyResponse expectedContactUsProxyResponse = getStubContactUsProxyResponse();
        given(responseService.buildResponse(eq(200), anyString())).willReturn(expectedContactUsProxyResponse);
        given(emailService.sendEmail(anyString(), anyString(), any())).willReturn("123");
        doNothing().when(dbService).putContactUsRequest(anyString(), any());
        AwsProxyRequest awsProxyRequest =
                OBJECT_MAPPER.readValue(
                        this.getClass().getClassLoader().getResourceAsStream("contact_us_request.json"),
                        AwsProxyRequest.class);

        // when
        ContactUsProxyResponse contactUsProxyResponse = app.handleRequest(awsProxyRequest, context);

        // then
        assertNotNull(contactUsProxyResponse);
        assertEquals(contactUsProxyResponse, expectedContactUsProxyResponse);
        verify(requestService).getAsPrettyString(any());
        verify(requestService).getContactUsRequest(any());
        verify(responseService).buildResponse(eq(200), anyString());
        verify(emailService).sendEmail(anyString(), anyString(), any());
        verify(dbService).putContactUsRequest(eq("123"), any());
    }

    @Test
    public void shouldReturnClientErrorResponse() throws IOException {
        // given
        ContactUsProxyResponse expectedContactUsProxyResponse = getStubContactUsProxyResponse();
        given(requestService.getAsPrettyString(any()))
                .willThrow(new ContactUsLambdaClientException("Client ex", new Exception("some ex")));
        given(responseService.buildResponse(eq(400), anyString())).willReturn(expectedContactUsProxyResponse);
        AwsProxyRequest awsProxyRequest =
                OBJECT_MAPPER.readValue(
                        this.getClass().getClassLoader().getResourceAsStream("contact_us_request.json"),
                        AwsProxyRequest.class);

        // when
        ContactUsProxyResponse contactUsProxyResponse = app.handleRequest(awsProxyRequest, context);

        // then
        assertNotNull(contactUsProxyResponse);
        assertEquals(contactUsProxyResponse, expectedContactUsProxyResponse);
        verify(responseService).buildResponse(eq(400), eq("Client error."));
        verifyZeroInteractions(emailService, dbService);
    }

    @Test
    public void shouldReturnServerErrorResponse() throws IOException {
        // given
        ContactUsProxyResponse expectedContactUsProxyResponse = getStubContactUsProxyResponse();
        given(emailService.sendEmail(anyString(), anyString(), any()))
                .willThrow(new RuntimeException("Client ex"));
        given(responseService.buildResponse(eq(500), anyString())).willReturn(expectedContactUsProxyResponse);
        AwsProxyRequest awsProxyRequest =
                OBJECT_MAPPER.readValue(
                        this.getClass().getClassLoader().getResourceAsStream("contact_us_request.json"),
                        AwsProxyRequest.class);

        // when
        ContactUsProxyResponse contactUsProxyResponse = app.handleRequest(awsProxyRequest, context);

        // then
        assertNotNull(contactUsProxyResponse);
        assertEquals(contactUsProxyResponse, expectedContactUsProxyResponse);
        verify(responseService).buildResponse(eq(500), eq("Server error."));
        verifyZeroInteractions(dbService);
    }

    @Test
    public void shouldOnlyWarmUpLambda() throws IOException {
        // given
        ContactUsProxyResponse expectedContactUsProxyResponse = getStubContactUsProxyResponse();
        given(responseService.buildWarmUpResponse()).willReturn(expectedContactUsProxyResponse);
        AwsProxyRequest awsProxyRequest =
                OBJECT_MAPPER.readValue(
                        this.getClass().getClassLoader().getResourceAsStream("warm_up_request.json"),
                        AwsProxyRequest.class);

        // when
        ContactUsProxyResponse contactUsProxyResponse = app.handleRequest(awsProxyRequest, context);

        // then
        assertNotNull(contactUsProxyResponse);
        assertEquals(contactUsProxyResponse, expectedContactUsProxyResponse);
        verify(requestService).getAsPrettyString(any());
        verify(responseService).buildWarmUpResponse();
        verifyNoMoreInteractions(requestService, responseService);
        verifyZeroInteractions(emailService, dbService);
    }

    private ContactUsProxyResponse getStubContactUsProxyResponse() {
        Map<String, String> headers = new HashMap<>();
        headers.put("test", "test");
        return new ContactUsProxyResponse(200, headers, "body");
    }
}