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
class ScheduleControllerIntegrationTest {

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

    private String createProviderAndGetToken() throws Exception {
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

        return loginAndGetToken("proveedor@test.com", "Password1!");
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

    // ── Público ──────────────────────────────────────────────────────────────

    @Test
    void getAvailableSchedules_publicEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/api/schedules/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAvailableSchedules_withDateFilter_returns200() throws Exception {
        mockMvc.perform(get("/api/schedules/available?date=2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAvailableSchedules_withServiceTypeFilter_returns200() throws Exception {
        mockMvc.perform(get("/api/schedules/available?serviceType=Peluquería"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAvailableSchedules_withProviderIdFilter_returns200() throws Exception {
        mockMvc.perform(get("/api/schedules/available?providerId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAvailableSchedules_withAllFilters_returns200() throws Exception {
        mockMvc.perform(get("/api/schedules/available?providerId=1&serviceType=Peluquería&date=2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── Protección por rol ───────────────────────────────────────────────────

    @Test
    void createSchedule_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-06-01T09:00:00",
                                  "endTime": "2026-06-01T10:00:00",
                                  "availableSlots": 5
                                }"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createSchedule_asCliente_returns403() throws Exception {
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-06-01T09:00:00",
                                  "endTime": "2026-06-01T10:00:00",
                                  "availableSlots": 5
                                }"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMySchedules_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/schedules/mine"))
                .andExpect(status().isUnauthorized());
    }

    // ── Creación de franja ───────────────────────────────────────────────────

    @Test
    void createSchedule_asProveedor_validData_returns200() throws Exception {
        String proveedorToken = createProviderAndGetToken();

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-06-01T09:00:00",
                                  "endTime": "2026-06-01T10:00:00",
                                  "availableSlots": 5
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.availableSlots").value(5))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createSchedule_invalidRange_returns400() throws Exception {
        String proveedorToken = createProviderAndGetToken();

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-06-01T10:00:00",
                                  "endTime": "2026-06-01T09:00:00",
                                  "availableSlots": 5
                                }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSchedule_overlapping_returns400() throws Exception {
        String proveedorToken = createProviderAndGetToken();

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-06-04T09:00:00",
                                  "endTime": "2026-06-04T11:00:00",
                                  "availableSlots": 5
                                }"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-06-04T10:00:00",
                                  "endTime": "2026-06-04T12:00:00",
                                  "availableSlots": 3
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── Listar mis franjas ───────────────────────────────────────────────────

    @Test
    void getMySchedules_asProveedor_returns200() throws Exception {
        String proveedorToken = createProviderAndGetToken();

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-06-02T09:00:00",
                                  "endTime": "2026-06-02T10:00:00",
                                  "availableSlots": 3
                                }"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/schedules/mine")
                        .header("Authorization", "Bearer " + proveedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].serviceType").value("Peluquería"));
    }

    // ── Toggle estado de franja ──────────────────────────────────────────────

    @Test
    void toggleScheduleStatus_asProveedor_returns200() throws Exception {
        String proveedorToken = createProviderAndGetToken();

        String scheduleResponse = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-06-03T09:00:00",
                                  "endTime": "2026-06-03T10:00:00",
                                  "availableSlots": 4
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long scheduleId = objectMapper.readTree(scheduleResponse).get("id").asLong();

        mockMvc.perform(patch("/api/schedules/" + scheduleId + "/status")
                        .header("Authorization", "Bearer " + proveedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void toggleScheduleStatus_notFound_returns404() throws Exception {
        String proveedorToken = createProviderAndGetToken();

        mockMvc.perform(patch("/api/schedules/99999/status")
                        .header("Authorization", "Bearer " + proveedorToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void toggleScheduleStatus_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/schedules/1/status"))
                .andExpect(status().isUnauthorized());
    }
}
