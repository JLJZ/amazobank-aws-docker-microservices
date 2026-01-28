package com.amazobank.crm.clientservice.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.amazobank.crm.clientservice.config.SqsProperties;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class SqsService {
    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    
    public SqsService(SqsClient sqsClient, SqsProperties sqsProperties) {
        this.sqsClient = sqsClient;
        this.sqsProperties = sqsProperties;
    }

    public void sendEmailNotification(String email, String messageBody) {

        // Create attribute for clientEmail
        MessageAttributeValue emailAttr = MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(email)
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(sqsProperties.getQueueUrl())
                .messageBody(messageBody)
                .messageGroupId("client-account") // required for FIFO queues
                .messageDeduplicationId(UUID.randomUUID().toString()) // required unless ContentBasedDeduplication = true
                .messageAttributes(Map.of(
                        "clientEmail", emailAttr
                ))
                .build();

        sqsClient.sendMessage(request);
    }

}
