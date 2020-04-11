package com.gralll.sam;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gralll.sam.exception.ContactUsLambdaClientException;
import com.gralll.sam.model.ContactUsProxyResponse;
import com.gralll.sam.model.ContactUsRequest;
import com.gralll.sam.service.AwsClientFactory;
import com.gralll.sam.service.DbService;
import com.gralll.sam.service.EmailService;
import com.gralll.sam.service.PropertyStorage;
import com.gralll.sam.service.RequestService;
import com.gralll.sam.service.ResponseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

import static java.lang.Boolean.FALSE;

/**
 * Request ad response types can be Input and Output stream or any custom objects.
 * Using streams directly helps to avoid different handling
 * when executing Lambda locally and in AWS environment.
 */
public class App implements RequestHandler<AwsProxyRequest, ContactUsProxyResponse> {

    // Use static variables to keep a context between executions
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AwsClientFactory awsClientFactory = new AwsClientFactory();
    private final EmailService emailService = new EmailService(awsClientFactory.getSesClient());
    private final DbService dbService = new DbService(awsClientFactory.getDynamoDB());
    private final ResponseService responseService = new ResponseService(objectMapper);
    private final RequestService requestService = new RequestService(objectMapper);
    private final PropertyStorage propertyStorage = new PropertyStorage();

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
        try {
            LOG.info("Request received.");
            LOG.debug(requestService.getAsPrettyString(request));

            return isWarmUpRequest(request)
                    ? handleWarmUpRequest()
                    : handleRealRequest(request);
        } catch (ContactUsLambdaClientException e) {
            LOG.error("Request was not handled due to a client error.", e);
            return responseService.buildResponse(400, "Client error.");
        } catch (Exception e) {
            LOG.error("Request was not handled due to a server error.", e);
            return responseService.buildResponse(500, "Server error.");
        }
    }

    private ContactUsProxyResponse handleRealRequest(AwsProxyRequest request) {
        // Parsing an input request
        ContactUsRequest contactUsRequest = requestService.getContactUsRequest(request);

        // Sending email to an agent
        String messageId = emailService.sendEmail(
                propertyStorage.getValue("SENDER_EMAIL"),
                propertyStorage.getValue("RECIPIENT_EMAIL"),
                contactUsRequest);
        LOG.info("ContactUs email message has been sent successfully.");

        // Saving request to DB
        dbService.putContactUsRequest(messageId, contactUsRequest);
        LOG.info("ContactUsRequest has been written to DB.");

        return responseService.buildResponse(200,
                String.format("Message %s has been sent successfully.", messageId));
    }

    private Boolean isWarmUpRequest(AwsProxyRequest request) {
        return Optional.ofNullable(request.getMultiValueHeaders())
                       .map(headers -> headers.containsKey("X-WARM-UP"))
                       .orElse(FALSE);
    }

    /**
     * Just to load a classpath and initialize all static fields for next calls.
     * Skips real AWS connections.
     *
     * @return Stub response
     */
    private ContactUsProxyResponse handleWarmUpRequest() {
        LOG.info("Lambda was warmed up.");
        return responseService.buildWarmUpResponse();
    }

}