package com.example.weather.kafka;

import com.example.kafka.EditContactEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

  @Bean
  public KafkaTemplate<String, EditContactEvent> editContactKafkaTemplate(ProducerFactory<String, EditContactEvent> factory) {
    return new KafkaTemplate<>(factory);
  }

  @Bean
  public NewTopic editContactTopic(@Value("${app.kafka.topics.editContact}") String topicName) {
    return TopicBuilder
        .name(topicName)
        .partitions(10)
        .replicas(3)
        .build();
  }

}
