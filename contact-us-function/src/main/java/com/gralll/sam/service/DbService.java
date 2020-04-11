package com.gralll.sam.service;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.gralll.sam.model.ContactUsRequest;

public class DbService {

    private static final String CONTACT_US_TABLE = "ContactUsTable";
    private final DynamoDB dynamoDB;

    public DbService(DynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
    }

    public void putContactUsRequest(String messageId, ContactUsRequest contactUsRequest) {
        dynamoDB.getTable(CONTACT_US_TABLE)
                          .putItem(new Item()
                                  .withPrimaryKey("Id", messageId)
                                  .withString("Subject", contactUsRequest.getSubject())
                                  .withString("Username", contactUsRequest.getUsername())
                                  .withString("Phone", contactUsRequest.getPhone())
                                  .withString("Email", contactUsRequest.getEmail())
                                  .withString("Question", contactUsRequest.getQuestion()));
    }
}
