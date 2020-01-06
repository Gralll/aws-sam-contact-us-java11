package com.gralll.sam;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gralll.sam.model.ContactUsProxyResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest(App.class)
@PowerMockIgnore({ "javax.management.*", "org.w3c.dom.*", "org.apache.log4j.*", "org.xml.sax.*", "javax.script.*", "com.sun.org.apache.xerces.*", "javax.xml.parsers.*"})
public class AppTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private App app;

    @Mock
    private Context context;
    @Mock
    private AwsClientFactory awsClientFactory;
    @Mock
    private AmazonSimpleEmailService amazonSimpleEmailService;
    @Mock
    private DynamoDB dynamoDB;
    @Mock
    private Table table;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        whenNew(AwsClientFactory.class).withNoArguments().thenReturn(awsClientFactory);
        mockStatic(System.class);
        when(System.getenv("RECIPIENT_EMAIL")).thenReturn("alex");
        given(awsClientFactory.getSesClient()).willReturn(amazonSimpleEmailService);
        given(awsClientFactory.getDynamoDB()).willReturn(dynamoDB);
        given(amazonSimpleEmailService.sendEmail(any(SendEmailRequest.class))).willReturn(new SendEmailResult().withMessageId("123"));
        given(dynamoDB.getTable(anyString())).willReturn(table);
        given(table.putItem(any(Item.class))).willReturn(null);
        app = new App();
    }

    @Test
    public void shouldReturnSuccessfulResponse() throws IOException {
        // given
        AwsProxyRequest awsProxyRequest =
                OBJECT_MAPPER.readValue(
                        this.getClass().getClassLoader().getResourceAsStream("contact_us_request.json"),
                        AwsProxyRequest.class);

        // when
        ContactUsProxyResponse contactUsProxyResponse = app.handleRequest(awsProxyRequest, context);

        // then
        assertNotNull(contactUsProxyResponse);
        assertThat(contactUsProxyResponse.getStatusCode(), is(200));
        assertThat(contactUsProxyResponse.getBody(), is("{\"response\":\"Message 123 has been sent successfully.\"}"));
    }

    @Test
    public void shouldOnlyWarmUpLambda() throws IOException {
        // given
        AwsProxyRequest awsProxyRequest =
                OBJECT_MAPPER.readValue(
                        this.getClass().getClassLoader().getResourceAsStream("warm_up_request.json"),
                        AwsProxyRequest.class);

        // when
        ContactUsProxyResponse contactUsProxyResponse = app.handleRequest(awsProxyRequest, context);

        // then
        assertNotNull(contactUsProxyResponse);
        assertThat(contactUsProxyResponse.getStatusCode(), is(201));
        assertThat(contactUsProxyResponse.getBody(), is("{\"response\":\"Lambda was warmed up. V1\"}"));
    }
}