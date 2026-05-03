package com.eap15.reservehub.controller;

import com.eap15.reservehub.dto.LoginRequestDTO;
import com.eap15.reservehub.dto.LoginResponseDTO;
import com.eap15.reservehub.dto.ProviderRegisterDTO;
import com.eap15.reservehub.dto.UserDTO;
import com.eap15.reservehub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/users", produces = "application/json")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // HU-01 Escenario 1: Registro como CLIENTE
    @PostMapping("/register/cliente")
    public ResponseEntity<UserDTO> registerCliente(@Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.registerCliente(userDTO));
    }

    // HU-01 Escenario 2: Registro como PROVEEDOR
    @PostMapping("/register/proveedor")
    public ResponseEntity<UserDTO> registerProveedor(
            @Valid @RequestBody ProviderRegisterDTO dto) {
        return ResponseEntity.ok(userService.registerProveedor(dto));
    }

    // HU-02: Inicio de sesion
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequest) {
        return ResponseEntity.ok(userService.login(loginRequest));
    }

    // HU-05: Obtener todos los usuarios (dashboard administrador)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // HU-03: Ver perfil por ID (Solo acceso propio o Admin)
    @PreAuthorize("#id == authentication.principal.user.id or hasRole('ADMINISTRADOR')")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // HU-03: Editar perfil (Solo dueño de la cuenta)
    @PreAuthorize("#id == authentication.principal.user.id")
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }

    // HU-04: Activar/desactivar cuenta (Solo administrador)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDTO> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }

    // HU-05: Dashboard específico por rol
    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/dashboard/cliente")
    public ResponseEntity<Map<String, String>> getClienteDashboard() {
        return ResponseEntity.ok(Map.of("message", "Bienvenido al Dashboard de Cliente", "reservas", "[]"));
    }

    @PreAuthorize("hasRole('PROVEEDOR')")
    @GetMapping("/dashboard/proveedor")
    public ResponseEntity<Map<String, String>> getProveedorDashboard() {
        return ResponseEntity.ok(Map.of("message", "Bienvenido al Dashboard de Proveedor", "agenda", "[]"));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/dashboard/admin")
    public ResponseEntity<Map<String, String>> getAdminDashboard() {
        return ResponseEntity.ok(Map.of("message", "Bienvenido al Panel de Control de Administración", "stats", "{}"));
    }
}
