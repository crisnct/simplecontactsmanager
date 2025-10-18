package com.example.contacts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.contacts.dto.ContactRequest;
import com.example.contacts.dto.ContactResponse;
import com.example.contacts.dto.WeatherInfo;
import com.example.contacts.model.Contact;
import com.example.contacts.model.User;
import com.example.contacts.repository.ContactRepository;
import com.example.contacts.repository.UserRepository;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ContactServiceImageProcessingTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WeatherClient weatherClient;

    @InjectMocks
    private ContactService contactService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User("alice", "secret", "ROLE_USER");
        owner.setId(42L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(weatherClient.fetchWeather(any())).thenReturn(new WeatherInfo("any", "clear", 20.0));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact contact = invocation.getArgument(0);
            contact.setId(123L);
            contact.setUpdatedAt(Instant.now());
            return contact;
        });
    }

    @Test
    void createShouldResizeOversizedImages() throws IOException {
        BufferedImage original = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_RGB);
        var graphics = original.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, 1000, 600);
        } finally {
            graphics.dispose();
        }
        MockMultipartFile picture = toMultipart("picture", "image/jpeg", original, "jpg");

        ContactRequest request = new ContactRequest();
        request.setName("John Doe");
        request.setAddress("123 Main St");
        request.setPicture(picture);

        ContactResponse response = contactService.create(request, "alice");

        assertNotNull(response);
        assertEquals("John Doe", response.getName());
        assertEquals("123 Main St", response.getAddress());

        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(contactCaptor.capture());
        Contact saved = contactCaptor.getValue();

        BufferedImage processed = ImageIO.read(new ByteArrayInputStream(saved.getPictureData()));
        assertNotNull(processed, "Processed image should be readable");
        int maxWidth = maxImageWidth();
        assertEquals(maxWidth, processed.getWidth(), "Image should be resized to max width");
        int expectedHeight = (int) Math.round(((double) maxWidth / 1000) * 600);
        assertEquals(expectedHeight, processed.getHeight(), "Image height should maintain aspect ratio");
        assertEquals("image/jpeg", saved.getPictureContentType());
    }

    @Test
    void createShouldKeepSmallerImagesUnchanged() throws IOException {
        BufferedImage original = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        var graphics = original.createGraphics();
        try {
            graphics.setColor(Color.GREEN);
            graphics.fillRect(0, 0, 400, 300);
        } finally {
            graphics.dispose();
        }
        MockMultipartFile picture = toMultipart("picture", "image/png", original, "png");

        ContactRequest request = new ContactRequest();
        request.setName("Jane Doe");
        request.setAddress("456 Elm St");
        request.setPicture(picture);

        contactService.create(request, "alice");

        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(contactCaptor.capture());
        Contact saved = contactCaptor.getValue();

        BufferedImage processed = ImageIO.read(new ByteArrayInputStream(saved.getPictureData()));
        assertNotNull(processed, "Processed image should be readable");
        assertEquals(400, processed.getWidth(), "Image width should remain unchanged");
        assertEquals(300, processed.getHeight(), "Image height should remain unchanged");
        assertEquals("image/png", saved.getPictureContentType());
    }

    private MockMultipartFile toMultipart(String name, String contentType, BufferedImage image, String format) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            return new MockMultipartFile(name, name + "." + format, contentType, baos.toByteArray());
        }
    }

    private int maxImageWidth() {
        try {
            Field field = ContactService.class.getDeclaredField("MAX_IMAGE_WIDTH");
            field.setAccessible(true);
            return field.getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to read MAX_IMAGE_WIDTH", e);
        }
    }
}
