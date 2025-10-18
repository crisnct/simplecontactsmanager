package com.example.contacts.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.contacts.ContactsServiceApplication;
import com.example.contacts.config.TestInfrastructureConfig;
import com.example.contacts.dto.ContactResponse;
import com.example.contacts.model.Contact;
import com.example.contacts.model.User;
import com.example.contacts.repository.ContactRepository;
import com.example.contacts.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
        classes = ContactsServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
                "spring.liquibase.enabled=false"
        }
)
@ImportAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@Import(TestInfrastructureConfig.class)
class ContactsWeatherSystemTest {

    private static final String WEATHER_BASE_URL = resolveWeatherBaseUrl();

    @DynamicPropertySource
    static void overrideWeatherBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("app.weather.base-url", () -> WEATHER_BASE_URL);
        registry.add("app.weather.cache-ttl-seconds", () -> 1);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private ContactRepository contactRepository;

    @MockBean
    private UserRepository userRepository;

    @BeforeAll
    static void verifyWeatherServiceReachable() {
        System.setProperty("net.bytebuddy.experimental", "true");
        RestTemplate client = new RestTemplate();
        try {
            client.getForEntity(WEATHER_BASE_URL + "/api/weather?location=health-check", String.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Weather service not reachable at " + WEATHER_BASE_URL
                    + ". Please ensure the weather service is running before executing this test.", ex);
        }
    }

    @Test
    void listContactsReturnsWeatherFromWeatherService() {
        User owner = new User("owner", "secret", "ROLE_USER");
        owner.setId(100L);

        Contact contact = new Contact("John Doe", "123 Main St", owner);
        contact.setId(1L);
        Instant updatedAt = Instant.parse("2024-10-18T10:15:30Z");
        contact.setCreatedAt(updatedAt);
        contact.setUpdatedAt(updatedAt);

        when(contactRepository.findAllByOrderByNameAsc()).thenReturn(List.of(contact));

        ContactResponse[] response = restTemplate.getForObject("/api/contacts", ContactResponse[].class);

       assertThat(response).isNotNull();
       assertThat(response).hasSize(1);
       ContactResponse contactResponse = response[0];
       assertThat(contactResponse.getName()).isEqualTo("John Doe");
       assertThat(contactResponse.getWeather())
                .as("Weather payload should be populated from weather service at %s", WEATHER_BASE_URL)
                .isNotNull();
       assertThat(contactResponse.getWeather().getLocation()).isEqualTo("123 Main St");
       assertThat(contactResponse.getWeather().getDescription())
                .as("Weather service response should not be fallback. Check connectivity to %s", WEATHER_BASE_URL)
                .isNotEqualTo("Weather service unavailable")
                .isNotBlank();
       assertThat(contactResponse.getWeather().getTemperatureCelsius()).isBetween(-50.0, 60.0);
    }

    private static String resolveWeatherBaseUrl() {
        String property = System.getProperty("test.weather.base-url");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String env = System.getenv("TEST_WEATHER_BASE_URL");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String host = System.getenv().getOrDefault("TEST_WEATHER_HOST", "localhost").trim();
        String port = System.getenv().getOrDefault("TEST_WEATHER_PORT", "9000").trim();
        return "http://" + host + ":" + port;
    }
}
