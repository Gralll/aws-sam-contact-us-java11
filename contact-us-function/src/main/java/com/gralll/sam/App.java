package com.gralll.sam;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gralll.sam.model.ContactUsProxyResponse;
import com.gralll.sam.model.ContactUsProxyResponse.ContactUsResponseBody;
import com.gralll.sam.model.ContactUsRequest;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

/**
 * Request ad response types can be Input and Output stream or any custom objects.
 * Using streams directly helps to avoid different handling
 * when executing Lambda locally and in AWS environment.
 */
public class App implements RequestHandler<AwsProxyRequest, ContactUsProxyResponse> {

    // Use static variables to keep a context between executions
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final AwsClientFactory AWS_CLIENT_FACTORY = new AwsClientFactory();

    // Use custom logger or logger from a Lambda context by
    // calling context.getLogger()
    private static final Logger LOG = LogManager.getLogger(App.class);

    /**
     * Send ContactUs form data to a specific email
     * and put the form data into database.
     * <p>
     *
     * @param request API Gateway request
     * @param context Lambda context
     * @return Proxy response to API Gateway
     */
    @Override
    public ContactUsProxyResponse handleRequest(AwsProxyRequest request, Context context) {

        LOG.info("Request was received");
        LOG.debug(getAsPrettyString(request));

        // WARMING UP
        // Allows to load a classpath and initialize all static fields for next executions
        // to improve performance
        if (Optional.ofNullable(request.getMultiValueHeaders()).map(headers -> headers.containsKey("X-WARM-UP")).orElse(FALSE)) {
            LOG.info("Lambda was warmed up");
            return buildResponse(201, "Lambda was warmed up. V1");
        }

        ContactUsRequest contactUsRequest = getContactUsRequest(request);

        SendEmailResult sendEmailResult = sendEmail(contactUsRequest);
        LOG.info("Email was sent");

        addEmailDetailsToDb(contactUsRequest, sendEmailResult);
        LOG.info("DB is updated");

        return buildResponse(200,
                String.format("Message %s has been sent successfully.", sendEmailResult.getMessageId()));
    }

    private void addEmailDetailsToDb(ContactUsRequest contactUsRequest, SendEmailResult sendEmailResult) {
        AWS_CLIENT_FACTORY.getDynamoDB().getTable("ContactUsTable")
                          .putItem(new Item()
                                  .withPrimaryKey("Id", sendEmailResult.getMessageId())
                                  .withString("Subject", contactUsRequest.getSubject())
                                  .withString("Username", contactUsRequest.getUsername())
                                  .withString("Phone", contactUsRequest.getPhone())
                                  .withString("Email", contactUsRequest.getEmail())
                                  .withString("Question", contactUsRequest.getQuestion()));
    }

    private String fillTemplate(String emailTemplate, ContactUsRequest contactUsRequest) {
        return String.format(
                emailTemplate,
                contactUsRequest.getUsername(),
                contactUsRequest.getEmail(),
                contactUsRequest.getPhone(),
                contactUsRequest.getQuestion());
    }

    private SendEmailResult sendEmail(ContactUsRequest contactUsRequest) {
        String emailTemplate = getEmailTemplate();
        String email = fillTemplate(emailTemplate, contactUsRequest);

        SendEmailRequest sendEmailRequest =
                new SendEmailRequest(
                        System.getenv("SENDER_EMAIL"),
                        new Destination(List.of(System.getenv("RECIPIENT_EMAIL"))),
                        new Message()
                                .withSubject(
                                        new Content()
                                                .withCharset(UTF_8.name())
                                                .withData(contactUsRequest.getSubject()))
                                .withBody(new Body()
                                        .withHtml(new Content()
                                                .withCharset(UTF_8.name())
                                                .withData(email))));
        LOG.info("Email template is ready");
        return AWS_CLIENT_FACTORY.getSesClient().sendEmail(sendEmailRequest);
    }

    private ContactUsProxyResponse buildResponse(int statusCode, String body) {
        ContactUsProxyResponse awsProxyResponse =
                new ContactUsProxyResponse();
        awsProxyResponse.setStatusCode(statusCode);
        awsProxyResponse.setBody(getBodyAsString(body));
        awsProxyResponse.addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        awsProxyResponse.addHeader("Access-Control-Allow-Origin", "*");
        return awsProxyResponse;
    }

    private String getBodyAsString(String body) {
        try {
            return OBJECT_MAPPER.writeValueAsString(new ContactUsResponseBody(body));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Writing ContactUsResponseBody as string failed.", e);
        }
    }

    private String getEmailTemplate() {
        try {
            return IOUtils.toString(
                    Objects.requireNonNull(this.getClass().getClassLoader()
                                               .getResourceAsStream("email_template.html")),
                    UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Loading an email template failed.", e);
        }
    }

    private ContactUsRequest getContactUsRequest(AwsProxyRequest request) {
        try {
            return OBJECT_MAPPER.readValue(request.getBody(), ContactUsRequest.class);
        } catch (IOException e) {
            throw new RuntimeException("ContactUsRequest deserialization failed.", e);
        }
    }

    private String getAsPrettyString(AwsProxyRequest request) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Writing AwsProxyRequest as string failed.", e);
        }
    }

}