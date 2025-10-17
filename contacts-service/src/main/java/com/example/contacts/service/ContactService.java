package com.example.contacts.service;

import com.example.contacts.dto.ContactRequest;
import com.example.contacts.dto.ContactResponse;
import com.example.contacts.dto.WeatherInfo;
import com.example.contacts.model.Contact;
import com.example.contacts.model.User;
import com.example.contacts.repository.ContactRepository;
import com.example.contacts.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
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

@Service
public class ContactService {

    private static final int MAX_IMAGE_WIDTH = 500;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");
    private static final Map<String, String> FORMAT_BY_CONTENT_TYPE = Map.ofEntries(
            Map.entry("image/png", "png"),
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/jpg", "jpg")
    );

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
        Contact contact = new Contact(request.getName(), request.getAddress(), owner);
        applyPicture(contact, request.getPicture());
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
        applyPicture(contact, request.getPicture());
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
        userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        List<Contact> contacts = contactRepository.findAllByOrderByNameAsc();
        StringBuilder builder = new StringBuilder();
        builder.append("name,address,pictureAvailable,owner,updatedAt\n");
        for (Contact contact : contacts) {
            builder.append(escape(contact.getName())).append(',')
                    .append(escape(contact.getAddress())).append(',')
                    .append(contact.getPictureData() != null && contact.getPictureData().length > 0 ? "yes" : "no").append(',')
                    .append(escape(contact.getOwner().getUsername())).append(',')
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
                contact.getPictureData() != null && contact.getPictureData().length > 0,
                contact.getOwner().getUsername(),
                contact.getUpdatedAt(),
                weather
        );
    }

    private void applyPicture(Contact contact, MultipartFile picture) {
        PicturePayload payload = processPicture(picture);
        if (payload != null) {
            contact.setPictureData(payload.data());
            contact.setPictureContentType(payload.contentType());
        }
    }

    private PicturePayload processPicture(MultipartFile picture) {
        if (picture == null || picture.isEmpty()) {
            return null;
        }
        String contentType = picture.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PNG and JPG images are supported.");
        }
        String formatName = FORMAT_BY_CONTENT_TYPE.get(contentType.toLowerCase());
        try {
            BufferedImage original = ImageIO.read(picture.getInputStream());
            if (original == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to process uploaded image.");
            }
            BufferedImage processed = resizeIfNecessary(original, formatName);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(processed, formatName, baos);
                return new PicturePayload(baos.toByteArray(), contentType);
            }
        } catch (IOException e) {
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

