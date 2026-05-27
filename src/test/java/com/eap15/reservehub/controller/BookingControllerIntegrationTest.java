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

    // ══════════════════════════════════════════════════════════════
    //  Helper methods
    // ══════════════════════════════════════════════════════════════

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

    /** Creates provider + schedule and returns the scheduleId as a string. */
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

    /** Creates provider + schedule and returns [scheduleId, proveedorToken]. */
    private String[] createProviderAndScheduleWithToken(int slots) throws Exception {
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
                                  "startTime": "2026-06-15T09:00:00",
                                  "endTime": "2026-06-15T10:00:00",
                                  "availableSlots": %d
                                }""".formatted(slots)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String scheduleId = objectMapper.readTree(scheduleResponse).get("id").asText();
        return new String[]{scheduleId, proveedorToken};
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

    /** Creates a booking and returns its ID as string. */
    private String createBookingAndGetId(String clientToken, String scheduleId) throws Exception {
        String resp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": " + scheduleId + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("id").asText();
    }

    // ══════════════════════════════════════════════════════════════
    //  Sprint 2 — HU-08: Creación de reservas
    // ══════════════════════════════════════════════════════════════

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

    @Test
    void createBooking_inactiveSchedule_returns400() throws Exception {
        String[] result = createProviderAndScheduleWithToken(5);
        String scheduleId = result[0];
        String proveedorToken = result[1];

        mockMvc.perform(patch("/api/schedules/" + scheduleId + "/status")
                        .header("Authorization", "Bearer " + proveedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        String clientToken = registerClientAndGetToken();

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": " + scheduleId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createBooking_noSlotsAvailable_returns400() throws Exception {
        String[] result = createProviderAndScheduleWithToken(1);
        String scheduleId = result[0];

        String clientToken = registerClientAndGetToken();

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": " + scheduleId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": " + scheduleId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ══════════════════════════════════════════════════════════════
    //  Sprint 3 — HU-10: Cancelación
    // ══════════════════════════════════════════════════════════════

    // HU-10 Escenario 1: cancelación exitosa
    @Test
    void cancelBooking_asOwner_returns200WithCancelledStatus() throws Exception {
        String scheduleId = createProviderAndSchedule();
        String clientToken = registerClientAndGetToken();
        String bookingId = createBookingAndGetId(clientToken, scheduleId);

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());
    }

    // HU-10 Escenario 2: reserva inexistente → 404
    @Test
    void cancelBooking_notFound_returns404() throws Exception {
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(patch("/api/bookings/99999/cancel")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isNotFound());
    }

    // HU-10 Escenario 3: no es el propietario → 403
    @Test
    void cancelBooking_notOwner_returns403() throws Exception {
        String scheduleId = createProviderAndSchedule();
        String clientToken = registerClientAndGetToken();
        String bookingId = createBookingAndGetId(clientToken, scheduleId);

        // Registrar un segundo cliente
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Otro",
                                  "lastName": "Cliente",
                                  "email": "otro@test.com",
                                  "password": "Password1!",
                                  "phone": "3009999999"
                                }"""))
                .andExpect(status().isOk());
        String otroToken = loginAndGetToken("otro@test.com", "Password1!");

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + otroToken))
                .andExpect(status().isForbidden());
    }

    // HU-10: cancelar sin token → 401
    @Test
    void cancelBooking_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/bookings/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════
    //  Sprint 3 — HU-10: Reagendamiento
    // ══════════════════════════════════════════════════════════════

    // HU-10 Escenario 4: reagendamiento exitoso
    @Test
    void rescheduleBooking_asOwner_returns200WithRescheduledStatus() throws Exception {
        String[] providerData = createProviderAndScheduleWithToken(5);
        String scheduleId = providerData[0];
        String proveedorToken = providerData[1];

        // Crear un segundo horario para reagendar
        String schedule2Response = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-07-01T14:00:00",
                                  "endTime": "2026-07-01T15:00:00",
                                  "availableSlots": 3
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String schedule2Id = objectMapper.readTree(schedule2Response).get("id").asText();

        String clientToken = registerClientAndGetToken();
        String bookingId = createBookingAndGetId(clientToken, scheduleId);

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/reschedule")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newScheduleId\": " + schedule2Id + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESCHEDULED"))
                .andExpect(jsonPath("$.scheduleId").value(Integer.parseInt(schedule2Id)))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    // HU-10 Escenario 5: nuevo horario sin cupos → 400
    @Test
    void rescheduleBooking_noSlotsInNewSchedule_returns400() throws Exception {
        String[] providerData = createProviderAndScheduleWithToken(5);
        String scheduleId = providerData[0];
        String proveedorToken = providerData[1];

        // Segundo horario con 0 cupos
        String schedule2Response = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-07-02T10:00:00",
                                  "endTime": "2026-07-02T11:00:00",
                                  "availableSlots": 1
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String schedule2Id = objectMapper.readTree(schedule2Response).get("id").asText();

        // Registrar segundo cliente para consumir el único cupo del nuevo horario
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Otro",
                                  "lastName": "Cli",
                                  "email": "cli2@test.com",
                                  "password": "Password1!",
                                  "phone": "3008888888"
                                }"""))
                .andExpect(status().isOk());
        String cli2Token = loginAndGetToken("cli2@test.com", "Password1!");
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + cli2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": " + schedule2Id + "}"))
                .andExpect(status().isOk());

        String clientToken = registerClientAndGetToken();
        String bookingId = createBookingAndGetId(clientToken, scheduleId);

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/reschedule")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newScheduleId\": " + schedule2Id + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // HU-10 Escenario 6: reagendar una reserva cancelada → 400
    @Test
    void rescheduleBooking_cancelledBooking_returns400() throws Exception {
        String[] providerData = createProviderAndScheduleWithToken(5);
        String scheduleId = providerData[0];
        String proveedorToken = providerData[1];

        String schedule2Response = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + proveedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "2026-08-01T10:00:00",
                                  "endTime": "2026-08-01T11:00:00",
                                  "availableSlots": 2
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String schedule2Id = objectMapper.readTree(schedule2Response).get("id").asText();

        String clientToken = registerClientAndGetToken();
        String bookingId = createBookingAndGetId(clientToken, scheduleId);

        // Cancelar primero
        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());

        // Intentar reagendar
        mockMvc.perform(patch("/api/bookings/" + bookingId + "/reschedule")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newScheduleId\": " + schedule2Id + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // HU-10: reagendar sin token → 401
    @Test
    void rescheduleBooking_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/bookings/1/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newScheduleId\": 2}"))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════
    //  Sprint 3 — HU-11: Historial con filtros
    // ══════════════════════════════════════════════════════════════

    // HU-11 Escenario 1: historial completo del cliente
    @Test
    void getMyBookings_withFilters_returnsByStatus() throws Exception {
        String scheduleId = createProviderAndSchedule();
        String clientToken = registerClientAndGetToken();
        String bookingId = createBookingAndGetId(clientToken, scheduleId);

        // Cancelar la reserva
        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());

        // Filtrar por CANCELLED → 1 resultado
        mockMvc.perform(get("/api/bookings/mine")
                        .param("status", "CANCELLED")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));
    }

    // HU-11 Escenario 2: sin reservas → lista vacía (no error)
    @Test
    void getMyBookings_noBookings_returnsEmptyList() throws Exception {
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(get("/api/bookings/mine")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // HU-11 Escenario 4: rango de fechas inválido → 400
    @Test
    void getMyBookings_invalidDateRange_returns400() throws Exception {
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(get("/api/bookings/mine")
                        .param("from", "2026-12-31T00:00:00")
                        .param("to", "2026-01-01T00:00:00")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // HU-11 Escenario 5: cliente no puede ver reservas de otro (endpoint solo retorna las propias)
    @Test
    void getMyBookings_onlyReturnsOwnBookings() throws Exception {
        String scheduleId = createProviderAndSchedule();
        String clientToken = registerClientAndGetToken();
        createBookingAndGetId(clientToken, scheduleId);

        // Segundo cliente: no ve reservas del primero
        mockMvc.perform(post("/api/users/register/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Otro",
                                  "lastName": "Cliente",
                                  "email": "otro2@test.com",
                                  "password": "Password1!",
                                  "phone": "3007777777"
                                }"""))
                .andExpect(status().isOk());
        String otroToken = loginAndGetToken("otro2@test.com", "Password1!");

        mockMvc.perform(get("/api/bookings/mine")
                        .header("Authorization", "Bearer " + otroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // HU-11 Escenario 6: proveedor ve sus propias reservas
    @Test
    void getProviderBookings_returnsOnlyProviderBookings() throws Exception {
        String[] providerData = createProviderAndScheduleWithToken(5);
        String scheduleId = providerData[0];
        String proveedorToken = providerData[1];

        String clientToken = registerClientAndGetToken();
        createBookingAndGetId(clientToken, scheduleId);

        mockMvc.perform(get("/api/bookings/provider/mine")
                        .header("Authorization", "Bearer " + proveedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    // HU-11 Escenario 6: cliente no puede acceder a ruta de proveedor → 403
    @Test
    void getProviderBookings_asCliente_returns403() throws Exception {
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(get("/api/bookings/provider/mine")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════
    //  Sprint 3 — HU-12: Reportes operativos
    // ══════════════════════════════════════════════════════════════

    // HU-12 Escenario 1: admin consulta reporte general → 200
    @Test
    void getAdminReport_asAdmin_returns200WithStats() throws Exception {
        String scheduleId = createProviderAndSchedule();
        String clientToken = registerClientAndGetToken();
        createBookingAndGetId(clientToken, scheduleId);

        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(get("/api/bookings/report")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.confirmed").value(1))
                .andExpect(jsonPath("$.cancelled").value(0));
    }

    // HU-12 Escenario 4: sin reservas en periodo → ceros (no error)
    @Test
    void getAdminReport_noData_returnsZeroes() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(get("/api/bookings/report")
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2020-01-31T23:59:59")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    // HU-12 Escenario 5: cliente intenta consultar reporte → 403
    @Test
    void getAdminReport_asCliente_returns403() throws Exception {
        String clientToken = registerClientAndGetToken();

        mockMvc.perform(get("/api/bookings/report")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    // HU-12 Escenario 6: rango de fechas inválido → 400
    @Test
    void getAdminReport_invalidDateRange_returns400() throws Exception {
        createAdmin();
        String adminToken = loginAndGetToken("admin@test.com", "Admin1234!");

        mockMvc.perform(get("/api/bookings/report")
                        .param("from", "2026-12-31T00:00:00")
                        .param("to", "2026-01-01T00:00:00")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // HU-12 Escenario 2: proveedor consulta su propio reporte
    @Test
    void getProviderReport_asProveedor_returns200WithOccupancy() throws Exception {
        String[] providerData = createProviderAndScheduleWithToken(5);
        String scheduleId = providerData[0];
        String proveedorToken = providerData[1];

        String clientToken = registerClientAndGetToken();
        createBookingAndGetId(clientToken, scheduleId);

        mockMvc.perform(get("/api/bookings/report/mine")
                        .header("Authorization", "Bearer " + proveedorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.confirmed").value(1))
                .andExpect(jsonPath("$.occupancy").isArray())
                .andExpect(jsonPath("$.occupancy[0].scheduleId").exists())
                .andExpect(jsonPath("$.occupancy[0].occupancyRate").exists());
    }

    // HU-12: proveedor no puede consultar reporte del admin
    @Test
    void getAdminReport_asProveedor_returns403() throws Exception {
        String[] providerData = createProviderAndScheduleWithToken(5);
        String proveedorToken = providerData[1];

        mockMvc.perform(get("/api/bookings/report")
                        .header("Authorization", "Bearer " + proveedorToken))
                .andExpect(status().isForbidden());
    }
}
