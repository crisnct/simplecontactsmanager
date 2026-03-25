package com.example.kafka;

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EditContactEvent {

  private String username;

  private Instant updatedAt;

  public EditContactEvent(String username) {
    this.username = username;
    this.updatedAt = Instant.now();
  }

}
