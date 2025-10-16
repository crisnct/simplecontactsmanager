package com.example.contacts.service;

import com.example.contacts.dto.ContactRequest;
import com.example.contacts.dto.ContactResponse;
import com.example.contacts.dto.WeatherInfo;
import com.example.contacts.model.Contact;
import com.example.contacts.model.User;
import com.example.contacts.repository.ContactRepository;
import com.example.contacts.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final WeatherClient weatherClient;

    public ContactService(ContactRepository contactRepository,
                          UserRepository userRepository,
                          WeatherClient weatherClient) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.weatherClient = weatherClient;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> listContacts(String search) {
        List<Contact> contacts;
        if (search != null && !search.isBlank()) {
            contacts = contactRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());
        } else {
            contacts = contactRepository.findAllByOrderByNameAsc();
        }
        return contacts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContactResponse create(ContactRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Contact contact = new Contact(request.getName(), request.getAddress(), request.getPictureUrl(), owner);
        Contact saved = contactRepository.save(contact);
        return toResponse(saved);
    }

    @Transactional
    public ContactResponse update(Long id, ContactRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Contact contact = contactRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new EntityNotFoundException("Contact not found"));
        contact.setName(request.getName());
        contact.setAddress(request.getAddress());
        contact.setPictureUrl(request.getPictureUrl());
        Contact updated = contactRepository.save(contact);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Contact contact = contactRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new EntityNotFoundException("Contact not found"));
        contactRepository.delete(contact);
    }

    @Transactional(readOnly = true)
    public String exportCsv(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        List<Contact> contacts = contactRepository.findByOwnerOrderByNameAsc(owner);
        StringBuilder builder = new StringBuilder();
        builder.append("name,address,pictureUrl,updatedAt\n");
        for (Contact contact : contacts) {
            builder.append(escape(contact.getName())).append(',')
                    .append(escape(contact.getAddress())).append(',')
                    .append(escape(contact.getPictureUrl())).append(',')
                    .append(contact.getUpdatedAt()).append('\n');
        }
        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private ContactResponse toResponse(Contact contact) {
        WeatherInfo weather = weatherClient.fetchWeather(contact.getAddress());
        return new ContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getAddress(),
                contact.getPictureUrl(),
                contact.getOwner().getUsername(),
                contact.getUpdatedAt(),
                weather
        );
    }
}
