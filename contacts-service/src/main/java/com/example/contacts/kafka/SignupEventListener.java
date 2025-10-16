package com.example.contacts.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SignupEventListener {

    private static final Logger log = LoggerFactory.getLogger(SignupEventListener.class);

    @KafkaListener(topics = "${app.kafka.topics.signup}", groupId = "contacts-service-signup-listener")
    public void onSignup(SignupEvent event) {
        log.info("Received signup event for user '{}', registered at {}", event.getUsername(), event.getRegisteredAt());
    }
}
