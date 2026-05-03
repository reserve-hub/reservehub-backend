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
class UserControllerIntegrationTest {

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

    // ── Registro de cliente ──────────────────────────────────────────────────

    @Test
    void registerCliente_validData_returns200() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Juan",
                                  "lastName": "Pérez",
                                  "email": "juan@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234567"
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("juan@test.com"))
                .andExpect(jsonPath("$.role").value("CLIENTE"));
    }

    @Test
    void registerCliente_duplicateEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Juan",
                                  "lastName": "Pérez",
                                  "email": "dup@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234567"
                                }"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Otro",
                                  "lastName": "Otro",
                                  "email": "dup@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234568"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registerCliente_invalidEmail_returns400WithValidationError() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Juan",
                                  "lastName": "Pérez",
                                  "email": "not-an-email",
                                  "password": "Password1!",
                                  "phone": "3001234567"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void registerCliente_missingFirstName_returns400() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lastName": "Pérez",
                                  "email": "juan2@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234567"
                                }"""))
                .andExpect(status().isBadRequest());
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokenAndRole() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ana",
                                  "lastName": "López",
                                  "email": "ana@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234569"
                                }"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@test.com\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("CLIENTE"));
    }

    @Test
    void login_wrongPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Pedro",
                                  "lastName": "Gómez",
                                  "email": "pedro@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234570"
                                }"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pedro@test.com\",\"password\":\"WrongPass1!\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── Endpoints protegidos ─────────────────────────────────────────────────

    @Test
    void getAllUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllUsers_asCliente_returns403() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Luis",
                                  "lastName": "Ríos",
                                  "email": "luis@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234571"
                                }"""))
                .andExpect(status().isOk());

        String clientToken = loginAndGetToken("luis@test.com", "Password1!");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_asAdmin_returns200WithList() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void toggleStatus_asAdmin_returns200() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        String registerBody = mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Carlos",
                                  "lastName": "Vera",
                                  "email": "carlos@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234572"
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(registerBody).get("id").asLong();

        mockMvc.perform(patch("/api/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void dashboardCliente_withClienteToken_returns200() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sofia",
                                  "lastName": "Ruiz",
                                  "email": "sofia@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234573"
                                }"""))
                .andExpect(status().isOk());

        String clientToken = loginAndGetToken("sofia@test.com", "Password1!");

        mockMvc.perform(get("/api/users/dashboard/cliente")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
