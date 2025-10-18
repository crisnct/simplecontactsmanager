package com.example.contacts.controller;

import com.example.contacts.dto.ContactRequest;
import com.example.contacts.dto.ContactResponse;
import com.example.contacts.service.ContactService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/contacts")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @GetMapping
    public List<ContactResponse> list(@RequestParam(value = "search", required = false) String search) {
        if (search == null || search.isBlank()) {
            log.info("Fetching contacts list without search filter");
        } else {
            log.info("Fetching contacts list with search='{}'", search);
        }
        return contactService.listContacts(search);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContactResponse> create(@Valid @ModelAttribute ContactRequest request,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User '{}' creating new contact '{}'", userDetails.getUsername(), request.getName());
        ContactResponse response = contactService.create(request, userDetails.getUsername());
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContactResponse> update(@PathVariable("id") Long id,
                                                  @Valid @ModelAttribute ContactRequest request,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User '{}' updating contact id={} with name='{}'", userDetails.getUsername(), id, request.getName());
        ContactResponse response = contactService.update(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User '{}' deleting contact id={}", userDetails.getUsername(), id);
        contactService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("User '{}' exporting contacts to CSV", userDetails.getUsername());
        String csv = contactService.exportCsv(userDetails.getUsername());
        String filename = URLEncoder.encode("contacts.csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/{id}/picture")
    public ResponseEntity<byte[]> getPicture(@PathVariable("id") Long id) {
        log.info("Loading picture for contact id={}", id);
        ContactService.PicturePayload payload = contactService.loadPicture(id);
        String contentType = payload.contentType();
        MediaType mediaType = (contentType != null && !contentType.isBlank())
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(payload.data());
    }
}
