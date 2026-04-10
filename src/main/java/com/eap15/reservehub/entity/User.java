package com.eap15.reservehub.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    // unique=true porque no pueden existir dos usuarios con el mismo correo
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String phone;

    // Solo aplica para usuarios con rol PROVEEDOR, null para clientes
    @Column(length = 100)
    private String serviceType;

    // Descripcion del servicio, solo para PROVEEDORES, requiere null para clientes
    @Column(columnDefinition = "TEXT")
    private String serviceDescription;

    // Enum almacenado como String en BD (más legible que número)
    // Valores posibles: CLIENTE, PROVEEDOR, ADMINISTRADOR
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // true = puede iniciar sesión / false = cuenta bloqueada (HU-02 escenario 4)
    @Column(nullable = false)
    private boolean active = true;

    // Enum interno para no usar Strings sueltos en el código
    public enum Role {
        CLIENTE, PROVEEDOR, ADMINISTRADOR
    }
}
