package com.example.contacts.repository;

import com.example.contacts.model.Contact;
import com.example.contacts.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    List<Contact> findAllByOrderByNameAsc();
    List<Contact> findByOwnerOrderByNameAsc(User owner);
    Optional<Contact> findByIdAndOwner(Long id, User owner);
    
    @Query("SELECT c FROM Contact c WHERE c.id = :id")
    Optional<Contact> findByIdWithPicture(@Param("id") Long id);
}
