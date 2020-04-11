package com.gralll.sam.service;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.gralll.sam.exception.ContactUsLambdaServerException;
import com.gralll.sam.model.ContactUsRequest;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public class EmailService {

    private static final Logger LOG = LogManager.getLogger(EmailService.class);

    private final AmazonSimpleEmailService simpleEmailService;

    public EmailService(AmazonSimpleEmailService simpleEmailService) {
        this.simpleEmailService = simpleEmailService;
    }

    public String sendEmail(String sourceEmail, String recipientEmail, ContactUsRequest contactUsRequest) {
        String emailTemplate = getEmailTemplate();
        String email = fillTemplate(emailTemplate, contactUsRequest);

        SendEmailRequest sendEmailRequest =
                new SendEmailRequest()
                        .withSource(sourceEmail)
                        .withDestination(new Destination(Collections.singletonList(recipientEmail)))
                        .withMessage(new Message()
                                .withSubject(
                                        new Content()
                                                .withCharset(UTF_8.name())
                                                .withData(contactUsRequest.getSubject()))
                                .withBody(new Body()
                                        .withHtml(new Content()
                                                .withCharset(UTF_8.name())
                                                .withData(email))));
        LOG.info("Email request is ready.");
        return simpleEmailService.sendEmail(sendEmailRequest).getMessageId();
    }

    private String fillTemplate(String emailTemplate, ContactUsRequest contactUsRequest) {
        return String.format(
                emailTemplate,
                contactUsRequest.getUsername(),
                contactUsRequest.getEmail(),
                contactUsRequest.getPhone(),
                contactUsRequest.getQuestion());
    }

    private String getEmailTemplate() {
        try {
            return IOUtils.toString(
                    Objects.requireNonNull(this.getClass().getClassLoader()
                                               .getResourceAsStream("email_template.html")),
                    UTF_8);
        } catch (IOException e) {
            throw new ContactUsLambdaServerException("Loading an email template failed.", e);
        }
    }
}
