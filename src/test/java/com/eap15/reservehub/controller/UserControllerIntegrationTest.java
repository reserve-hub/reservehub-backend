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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    // ── Registro de proveedor ────────────────────────────────────────────────

    @Test
    void registerProveedor_validCode_returns200WithProveedorRole() throws Exception {
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
                                  "firstName": "Carlos",
                                  "lastName": "Proveedor",
                                  "email": "carlos.prov@test.com",
                                  "password": "Password1!",
                                  "phone": "3109876543",
                                  "serviceType": "Barbería",
                                  "providerCode": "%s"
                                }""".formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROVEEDOR"))
                .andExpect(jsonPath("$.email").value("carlos.prov@test.com"));
    }

    @Test
    void registerProveedor_invalidCode_returns400() throws Exception {
        mockMvc.perform(post("/api/users/register/proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "User",
                                  "email": "test.prov@test.com",
                                  "password": "Password1!",
                                  "phone": "3109876544",
                                  "serviceType": "Barbería",
                                  "providerCode": "INVALID-CODE"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registerProveedor_duplicateEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Existing",
                                  "lastName": "User",
                                  "email": "existing@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234567"
                                }"""))
                .andExpect(status().isOk());

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
                                  "firstName": "Dup",
                                  "lastName": "User",
                                  "email": "existing@test.com",
                                  "password": "Password1!",
                                  "phone": "3109876543",
                                  "serviceType": "Test",
                                  "providerCode": "%s"
                                }""".formatted(code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
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

    @Test
    void login_disabledAccount_returns400() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Disabled",
                                  "lastName": "User",
                                  "email": "disabled@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234579"
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(registerResponse).get("id").asLong();

        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(patch("/api/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"disabled@test.com\",\"password\":\"Password1!\"}"))
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

    // ── getUserById ──────────────────────────────────────────────────────────

    @Test
    void getUserById_ownProfile_returns200() throws Exception {
        String registerBody = mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sofia",
                                  "lastName": "Vargas",
                                  "email": "sofia.v@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234575"
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(registerBody).get("id").asLong();
        String token = loginAndGetToken("sofia.v@test.com", "Password1!");

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sofia.v@test.com"));
    }

    @Test
    void getUserById_asAdmin_returns200() throws Exception {
        String registerBody = mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Target",
                                  "lastName": "User",
                                  "email": "target@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234576"
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(registerBody).get("id").asLong();

        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("target@test.com"));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(get("/api/users/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── updateUser ───────────────────────────────────────────────────────────

    @Test
    void updateUser_ownProfile_returns200() throws Exception {
        String registerBody = mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Original",
                                  "lastName": "Name",
                                  "email": "original@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234577"
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(registerBody).get("id").asLong();
        String token = loginAndGetToken("original@test.com", "Password1!");

        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updated",
                                  "lastName": "Name",
                                  "email": "original@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234578"
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void updateUser_emailAlreadyTaken_returns400() throws Exception {
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Other",
                                  "lastName": "User",
                                  "email": "taken@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234580"
                                }"""))
                .andExpect(status().isOk());

        String registerBody = mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updater",
                                  "lastName": "User",
                                  "email": "updater@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234581"
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(registerBody).get("id").asLong();
        String token = loginAndGetToken("updater@test.com", "Password1!");

        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updater",
                                  "lastName": "User",
                                  "email": "taken@test.com",
                                  "password": "Password1!",
                                  "phone": "3001234581"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── Dashboards ───────────────────────────────────────────────────────────

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

    @Test
    void dashboardProveedor_withProveedorToken_returns200() throws Exception {
        String proveedorToken = createProviderAndGetToken();

        mockMvc.perform(get("/api/users/dashboard/proveedor")
                        .header("Authorization", "Bearer " + proveedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void dashboardAdmin_withAdminToken_returns200() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(get("/api/users/dashboard/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void dashboardCliente_withProveedorToken_returns403() throws Exception {
        String proveedorToken = createProviderAndGetToken();

        mockMvc.perform(get("/api/users/dashboard/cliente")
                        .header("Authorization", "Bearer " + proveedorToken))
                .andExpect(status().isForbidden());
    }
}
