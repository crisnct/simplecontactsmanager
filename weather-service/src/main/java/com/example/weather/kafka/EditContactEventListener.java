package com.example.weather.kafka;

import com.example.kafka.EditContactEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EditContactEventListener {

  @KafkaListener(
    topics = "${app.kafka.topics.editContact}",
    groupId = "weather-service-edit-listener",
    containerFactory = "kafkaListenerContainerFactory"
  )
  public void onEdit(EditContactEvent event) {
    try {
      log.info("Received edit contact event for user '{}', registered at {}", event.getUsername(), event.getUpdatedAt());
    } catch (IllegalArgumentException e) {
      // nu retry → ok
      log.warn("Invalid event: {}", event);
    } catch (Exception e) {
      // IMPORTANT → rethrow
      throw e;
    }
  }

}
