package com.example.contacts.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@TestConfiguration
public class TestInfrastructureConfig {

    @Bean
    PlatformTransactionManager transactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
                // no-op
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                // no-op
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
                // no-op
            }
        };
    }

    @Bean
    KafkaProperties kafkaProperties() {
        KafkaProperties properties = new KafkaProperties();
        properties.getBootstrapServers().clear();
        properties.getBootstrapServers().add("localhost:65535");
        properties.getAdmin().setFailFast(false);
        properties.getAdmin().setAutoCreate(false);
        return properties;
    }
}

