package com.example.weather.kafka;

import com.example.kafka.EditContactEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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

  @Bean
  public DefaultErrorHandler errorHandler(KafkaTemplate<String, EditContactEvent> kafkaTemplate) {

    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        kafkaTemplate,
        (record, ex) -> {
          // trimite în topic.DLT pe același partition
          return new TopicPartition(record.topic() + ".DLT", record.partition());
        }
    );

    FixedBackOff backOff = new FixedBackOff(2000L, 3); // 3 retry-uri

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    // NU retry pentru erori de business
    errorHandler.addNotRetryableExceptions(
        IllegalArgumentException.class
    );

    // 🔥 IMPORTANT: log clar când trimite în DLT
    errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
      System.out.println("Retry attempt " + deliveryAttempt +
          " for topic=" + record.topic() +
          ", partition=" + record.partition() +
          ", offset=" + record.offset());
    });

    return errorHandler;
  }

  @Bean(name = "kafkaListenerContainerFactory")
  public ConcurrentKafkaListenerContainerFactory<String, EditContactEvent> kafkaListenerContainerFactory(
      ConsumerFactory<String, EditContactEvent> consumerFactory,
      DefaultErrorHandler errorHandler
  ) {

    ConcurrentKafkaListenerContainerFactory<String, EditContactEvent> factory
        = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

    return factory;
  }


}
