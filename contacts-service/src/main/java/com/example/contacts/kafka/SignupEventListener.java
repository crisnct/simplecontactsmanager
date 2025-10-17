package com.example.contacts.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SignupEventListener {

  @KafkaListener(topics = "${app.kafka.topics.signup}", groupId = "contacts-service-signup-listener")
  public void onSignup(SignupEvent event) {
    log.info("Received signup event for user '{}', registered at {}", event.getUsername(), event.getRegisteredAt());
  }
}
