package com.example.contacts.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.contacts.config.SecurityConfig;
import com.example.contacts.dto.ContactResponse;
import com.example.contacts.dto.WeatherInfo;
import com.example.contacts.service.ContactService;
import com.example.contacts.service.DatabaseUserDetailsService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContactController.class)
@Import(SecurityConfig.class)
class ContactControllerIntegrationTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactService contactService;

    @MockBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @Test
    void exportContactsReturnsCsvForAuthenticatedUser() throws Exception {
        String csv = "name,address,pictureAvailable,updatedAt\nJohn Doe,123 Main St,no,2024-10-18T08:30:00Z\n";
        when(contactService.exportCsv("alice")).thenReturn(csv);

        mockMvc.perform(get("/api/contacts/export")
                        .with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("contacts.csv")))
                .andExpect(content().string(csv));

        verify(contactService).exportCsv("alice");
    }

    @Test
    void listContactsReturnsPayloadFromService() throws Exception {
        ContactResponse response = new ContactResponse(
                1L,
                "John Doe",
                "123 Main St",
                false,
                "owner",
                Instant.parse("2024-10-18T08:30:00Z"),
                new WeatherInfo("Test City", "Sunny", 21.5)
        );
        when(contactService.listContacts("john")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/contacts").param("search", "john"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].address").value("123 Main St"))
                .andExpect(jsonPath("$[0].ownerUsername").value("owner"))
                .andExpect(jsonPath("$[0].weather.description").value("Sunny"))
                .andExpect(jsonPath("$[0].weather.temperatureCelsius").value(21.5));

        verify(contactService).listContacts(eq("john"));
    }
}
