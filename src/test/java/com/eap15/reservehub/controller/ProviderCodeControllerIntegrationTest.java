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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProviderCodeControllerIntegrationTest {

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
    void generateCode_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/provider-codes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCodes_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/provider-codes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivateCode_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/provider-codes/1/deactivate"))
                .andExpect(status().isUnauthorized());
    }

    // ── Rol incorrecto ───────────────────────────────────────────────────────

    @Test
    void generateCode_asCliente_returns403() throws Exception {
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(post("/api/provider-codes")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listCodes_asCliente_returns403() throws Exception {
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(get("/api/provider-codes")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    // ── Admin puede generar y listar ─────────────────────────────────────────

    @Test
    void generateCode_asAdmin_returns200WithCode() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(post("/api/provider-codes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.used").value(false))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void listCodes_asAdmin_returns200WithList() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(post("/api/provider-codes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/provider-codes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").exists());
    }

    @Test
    void deactivateCode_asAdmin_returns200WithInactiveCode() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        String codeResponse = mockMvc.perform(post("/api/provider-codes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long codeId = objectMapper.readTree(codeResponse).get("id").asLong();

        mockMvc.perform(patch("/api/provider-codes/" + codeId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivateCode_notFound_returns404() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(patch("/api/provider-codes/99999/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void deactivateCode_alreadyUsed_returns400() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        // Generate code
        String codeResponse = mockMvc.perform(post("/api/provider-codes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String code = objectMapper.readTree(codeResponse).get("code").asText();
        Long codeId = objectMapper.readTree(codeResponse).get("id").asLong();

        // Use the code by registering a provider
        mockMvc.perform(post("/api/users/register/proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Prov",
                                  "lastName": "Used",
                                  "email": "prov.used@test.com",
                                  "password": "Password1!",
                                  "phone": "3109876543",
                                  "serviceType": "Test",
                                  "providerCode": "%s"
                                }""".formatted(code)))
                .andExpect(status().isOk());

        // Try to deactivate the already-used code
        mockMvc.perform(patch("/api/provider-codes/" + codeId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
