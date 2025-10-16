package com.example.contacts.config;

import com.example.contacts.kafka.SignupEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.config.TopicBuilder;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, SignupEvent> signupProducerFactory(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, SignupEvent> signupKafkaTemplate(ProducerFactory<String, SignupEvent> signupProducerFactory) {
        return new KafkaTemplate<>(signupProducerFactory);
    }

    @Bean
    public NewTopic signupTopic(@Value("${app.kafka.topics.signup}") String topicName) {
        return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
    }
}
