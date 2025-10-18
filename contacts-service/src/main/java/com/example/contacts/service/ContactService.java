package com.example.contacts.service;

import com.example.contacts.dto.ContactRequest;
import com.example.contacts.dto.ContactResponse;
import com.example.contacts.dto.WeatherInfo;
import com.example.contacts.model.Contact;
import com.example.contacts.model.User;
import com.example.contacts.repository.ContactRepository;
import com.example.contacts.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class ContactService {

    private static final int MAX_IMAGE_WIDTH = 500;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");
    private static final Map<String, String> FORMAT_BY_CONTENT_TYPE = Map.ofEntries(
            Map.entry("image/png", "png"),
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/jpg", "jpg")
    );

    @Autowired
    private ContactRepository contactRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WeatherClient weatherClient;

    @Transactional(readOnly = true)
    public List<ContactResponse> listContacts(String search) {
        if (search == null || search.isBlank()) {
            log.info("Listing all contacts");
        } else {
            log.info("Listing contacts with search='{}'", search);
        }
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
        log.info("Creating new contact '{}' for user '{}'", request.getName(), username);
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Contact contact = new Contact(request.getName(), request.getAddress(), owner);
        applyPicture(contact, request.getPicture());
        Contact saved = contactRepository.save(contact);
        log.info("Contact id={} created for user '{}'", saved.getId(), username);
        return toResponse(saved);
    }

    @Transactional
    public ContactResponse update(Long id, ContactRequest request, String username) {
        log.info("Updating contact id={} for user '{}'", id, username);
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Contact contact = contactRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new EntityNotFoundException("Contact not found"));
        contact.setName(request.getName());
        contact.setAddress(request.getAddress());
        applyPicture(contact, request.getPicture());
        Contact updated = contactRepository.save(contact);
        log.info("Contact id={} updated for user '{}'", id, username);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id, String username) {
        log.info("Deleting contact id={} for user '{}'", id, username);
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Contact contact = contactRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new EntityNotFoundException("Contact not found"));
        contactRepository.delete(contact);
        log.info("Contact id={} deleted", id);
    }

    @Transactional(readOnly = true)
    public String exportCsv(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        log.info("Generating CSV export for user '{}'", username);
        List<Contact> contacts = contactRepository.findByOwnerOrderByNameAsc(owner);
        StringBuilder builder = new StringBuilder();
        builder.append("name,address,pictureAvailable,updatedAt\n");
        for (Contact contact : contacts) {
            builder.append(escape(contact.getName())).append(',')
                    .append(escape(contact.getAddress())).append(',')
                    .append(contact.getPictureData() != null && contact.getPictureData().length > 0 ? "yes" : "no").append(',')
                    .append(contact.getUpdatedAt()).append('\n');
        }
        log.info("CSV export generated with {} contacts", contacts.size());
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
                contact.getPictureData() != null && contact.getPictureData().length > 0,
                contact.getOwner().getUsername(),
                contact.getUpdatedAt(),
                weather
        );
    }

    private void applyPicture(Contact contact, MultipartFile picture) {
        PicturePayload payload = processPicture(picture);
        if (payload != null) {
            log.info("Applying picture ({} bytes, {}) to contact id={}",
                    payload.data().length, payload.contentType(), contact.getId());
            contact.setPictureData(payload.data());
            contact.setPictureContentType(payload.contentType());
        }
    }

    private PicturePayload processPicture(MultipartFile picture) {
        if (picture == null || picture.isEmpty()) {
            log.info("No picture provided for processing");
            return null;
        }
        String contentType = picture.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Rejected picture with unsupported content type '{}'", contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PNG and JPG images are supported.");
        }
        String formatName = FORMAT_BY_CONTENT_TYPE.get(contentType.toLowerCase());
        try {
            BufferedImage original = ImageIO.read(picture.getInputStream());
            if (original == null) {
                log.warn("Uploaded file could not be read as an image");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to process uploaded image.");
            }
            BufferedImage processed = resizeIfNecessary(original, formatName);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(processed, formatName, baos);
                return new PicturePayload(baos.toByteArray(), contentType);
            }
        } catch (IOException e) {
            log.error("Failed to read uploaded image: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded image.");
        }
    }

    private BufferedImage resizeIfNecessary(BufferedImage source, String formatName) {
        if (source.getWidth() <= MAX_IMAGE_WIDTH) {
            return ensureCompatibleImage(source, formatName);
        }
        int newWidth = MAX_IMAGE_WIDTH;
        int newHeight = (int) Math.round(((double) newWidth / source.getWidth()) * source.getHeight());
        int imageType = "png".equals(formatName) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(newWidth, newHeight, imageType);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Image scaled = source.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            graphics.drawImage(scaled, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private BufferedImage ensureCompatibleImage(BufferedImage source, String formatName) {
        boolean needsConversion = ("png".equals(formatName) && source.getType() != BufferedImage.TYPE_INT_ARGB)
                || ("jpg".equals(formatName) && source.getType() != BufferedImage.TYPE_INT_RGB);
        if (!needsConversion) {
            return source;
        }
        int imageType = "png".equals(formatName) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage converted = new BufferedImage(source.getWidth(), source.getHeight(), imageType);
        Graphics2D graphics = converted.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return converted;
    }

    @Transactional(readOnly = true)
    public PicturePayload loadPicture(Long id) {
        log.info("Loading picture data for contact id={}", id);
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        if (contact.getPictureData() == null || contact.getPictureData().length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Picture not found");
        }
        return new PicturePayload(contact.getPictureData(), contact.getPictureContentType());
    }

    public record PicturePayload(byte[] data, String contentType) {
    }
}


