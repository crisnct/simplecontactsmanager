package com.example.contacts.service;

import com.example.contacts.dto.SignupRequest;
import com.example.contacts.kafka.SignupEvent;
import com.example.contacts.model.User;
import com.example.contacts.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private KafkaTemplate<String, SignupEvent> kafkaTemplate;
    @Value("${app.kafka.topics.signup}")
    private String signupTopic;

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
        log.info("User '{}' persisted, publishing signup event", saved.getUsername());
        kafkaTemplate.send(signupTopic, new SignupEvent(saved.getUsername()));
        return saved;
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        log.info("Fetching user by username '{}'", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}

