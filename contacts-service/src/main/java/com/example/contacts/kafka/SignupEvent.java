package com.example.contacts.kafka;

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SignupEvent {

  private String username;
  private Instant registeredAt = Instant.now();

  public SignupEvent(String username) {
    this.username = username;
    this.registeredAt = Instant.now();
  }

}
