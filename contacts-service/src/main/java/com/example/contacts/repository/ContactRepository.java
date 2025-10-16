package com.example.contacts.repository;

import com.example.contacts.model.Contact;
import com.example.contacts.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    List<Contact> findAllByOrderByNameAsc();
    List<Contact> findByOwnerOrderByNameAsc(User owner);
    Optional<Contact> findByIdAndOwner(Long id, User owner);
}
