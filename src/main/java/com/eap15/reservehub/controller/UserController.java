package com.eap15.reservehub.controller;

import com.eap15.reservehub.dto.UserDTO;
import com.eap15.reservehub.dto.ProviderRegisterDTO;
import com.eap15.reservehub.entity.User;
import com.eap15.reservehub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/users", produces = "application/json")
public class UserController {

    @Autowired
    private UserService userService;

    // HU-01 Escenario 1: Registro como CLIENTE
    // POST /api/users/register/cliente
    @PostMapping("/register/cliente")
    public ResponseEntity<UserDTO> registerCliente(@Valid @RequestBody UserDTO userDTO) {
        // @Valid activa las validaciones del DTO antes de entrar al método
        // Si algo falla, GlobalExceptionHandler lo captura automáticamente
        return ResponseEntity.ok(userService.registerCliente(userDTO));
    }

    // HU-01 Escenario 2: Registro como PROVEEDOR
    // POST /api/users/register/proveedor
    @PostMapping("/register/proveedor")
    public ResponseEntity<UserDTO> registerProveedor(
            @Valid @RequestBody ProviderRegisterDTO dto) {
        return ResponseEntity.ok(userService.registerProveedor(dto));
    }

    // HU-05: Obtener todos los usuarios (dashboard administrador)
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // HU-03: Ver perfil por ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // HU-03: Editar perfil
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }

    // HU-04: Activar/desactivar cuenta
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDTO> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }
}