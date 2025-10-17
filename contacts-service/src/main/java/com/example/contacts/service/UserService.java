package com.example.contacts.service;

import com.example.contacts.dto.SignupRequest;
import com.example.contacts.kafka.SignupEvent;
import com.example.contacts.model.User;
import com.example.contacts.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, SignupEvent> kafkaTemplate;
    private final String signupTopic;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       KafkaTemplate<String, SignupEvent> kafkaTemplate,
                       @Value("${app.kafka.topics.signup}") String signupTopic) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.kafkaTemplate = kafkaTemplate;
        this.signupTopic = signupTopic;
    }

    @Transactional
    public User register(SignupRequest request) {
        log.info("Registering new user '{}'", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username '{}' is already taken", request.getUsername());
            throw new IllegalArgumentException("Username already taken");
        }
        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                "ROLE_USER"
        );
        User saved = userRepository.save(user);
        log.debug("User '{}' persisted, publishing signup event", saved.getUsername());
        kafkaTemplate.send(signupTopic, new SignupEvent(saved.getUsername()));
        return saved;
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        log.debug("Fetching user by username '{}'", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
