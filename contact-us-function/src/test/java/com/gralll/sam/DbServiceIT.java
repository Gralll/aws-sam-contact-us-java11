package com.gralll.sam;

import cloud.localstack.LocalstackTestRunner;
import cloud.localstack.TestUtils;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.gralll.sam.model.ContactUsRequest;
import com.gralll.sam.service.DbService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(LocalstackTestRunner.class)
@LocalstackDockerProperties(services = { "dynamodb" })
public class DbServiceIT {

    private DynamoDB dynamoDB;
    private DbService dbService;

    @Before
    public void setUp() {
        AmazonDynamoDB clientDynamoDB = TestUtils.getClientDynamoDB();
        dynamoDB = new DynamoDB(clientDynamoDB);
        dbService = new DbService(dynamoDB);

        dynamoDB.createTable(
                new CreateTableRequest()
                        .withTableName("ContactUsTable")
                        .withKeySchema(new KeySchemaElement("Id", KeyType.HASH))
                        .withAttributeDefinitions(new AttributeDefinition("Id", ScalarAttributeType.S))
                        .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L)));
    }

    @After
    public void tearDown() {
        dynamoDB.getTable("ContactUsTable").delete();
    }

    @Test
    public void shouldAddContactUsRequestToDbTable() {
        // given
        ContactUsRequest contactUsRequest = new ContactUsRequest("subject", "name", "+79991234545", "123@mail.ru", "Qeustion");

        // when
        dbService.putContactUsRequest("123", contactUsRequest);

        // then
        Item item = dynamoDB.getTable("ContactUsTable").getItem(new PrimaryKey("Id", "123"));
        assertEquals(item.get("Subject"), contactUsRequest.getSubject());
        assertEquals(item.get("Username"), contactUsRequest.getUsername());
        assertEquals(item.get("Phone"), contactUsRequest.getPhone());
        assertEquals(item.get("Email"), contactUsRequest.getEmail());
        assertEquals(item.get("Question"), contactUsRequest.getQuestion());
    }
}
