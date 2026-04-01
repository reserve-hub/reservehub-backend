package com.eap15.reservehub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequestDTO {

    @NotBlank(message = "El correo es requerido")
    @Email(message = "El formato del correo no es valido")
    private String email;

    @NotBlank(message = "La contrasena es requerida")
    private String password;

    public LoginRequestDTO() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
