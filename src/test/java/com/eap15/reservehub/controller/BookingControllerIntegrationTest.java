package com.eap15.reservehub.controller;

import com.eap15.reservehub.entity.User;
import com.eap15.reservehub.repository.BookingRepository;
import com.eap15.reservehub.repository.ProviderCodeRepository;
import com.eap15.reservehub.repository.ScheduleRepository;
import com.eap15.reservehub.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired ProviderCodeRepository providerCodeRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanup() {
        bookingRepository.deleteAll();
        scheduleRepository.deleteAll();
        providerCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createAdmin() {
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("Test");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("Admin1234!"));
        admin.setPhone("3000000001");
        admin.setRole(User.Role.ADMINISTRADOR);
        admin.setActive(true);
        return userRepository.save(admin);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("token").asText();
    }

    private String createProviderAndSchedule() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        String codeResponse = mockMvc.perform(post("/api/provider-codes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(codeResponse).get("code").asText();

        mockMvc.perform(post("/api/users/register/proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "María",
                                  "lastName": "García",
                                  "email": "proveedor@test.com",
                                  "password": "Password1!",
                                  "phone": "3109876543",
                                  "serviceType": "Peluquería",
                                  "providerCode": "%s"
                                }""".formatted(code)))
                .andExpect(status().isOk());

        String proveedorToken = loginAndGetToken("proveedor@test.com", "Password1!");

        String scheduleResponse = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-06-10T09:00:00",
                                  "endTime": "2026-06-10T10:00:00",
                                  "availableSlots": 5
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(scheduleResponse).get("id").asText();
    }

    private String registerClientAndGetToken() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Cliente",
                                  "lastName": "Test",
                                  "email": "cliente@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234567"
                                }"""))
                .andExpect(status().isOk());
        return loginAndGetToken("cliente@test.com", "Password1!");
    }

    // ── Sin autenticación ────────────────────────────────────────────────────

    @Test
    void createBooking_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyBookings_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/bookings/mine"))
                .andExpect(status().isUnauthorized());
    }

    // ── Rol incorrecto ───────────────────────────────────────────────────────

    @Test
    void createBooking_asProveedor_returns403() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");
        String codeResponse = mockMvc.perform(post("/api/provider-codes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(codeResponse).get("code").asText();

        mockMvc.perform(post("/api/users/register/proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Prov",
                                  "lastName": "Test",
                                  "email": "prov2@test.com",
                                  "password": "Password1!",
                                  "phone": "3109876544",
                                  "serviceType": "Barbería",
                                  "providerCode": "%s"
                                }""".formatted(code)))
                .andExpect(status().isOk());
        String proveedorToken = loginAndGetToken("prov2@test.com", "Password1!");

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": 1}"))
                .andExpect(status().isForbidden());
    }

    // ── Flujo exitoso ────────────────────────────────────────────────────────

    @Test
    void createBooking_asCliente_validSchedule_returns200() throws Exception {
        String scheduleId = createProviderAndSchedule();
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": " + scheduleId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getMyBookings_asCliente_returns200WithList() throws Exception {
        String scheduleId = createProviderAndSchedule();
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": " + scheduleId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bookings/mine")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void createBooking_nonExistentSchedule_returns400() throws Exception {
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": 99999}"))
                .andExpect(status().isBadRequest());
    }
}
