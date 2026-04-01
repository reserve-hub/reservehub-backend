package com.eap15.reservehub.dto;

import com.eap15.reservehub.entity.User;

public class LoginResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private User.Role role;
    private String message;

    public LoginResponseDTO() {}

    // Constructor completo para construirlo facilmente en el service
    public LoginResponseDTO(Long id, String firstName, String lastName,
                            String email, User.Role role, String message) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public User.Role getRole() { return role; }
    public void setRole(User.Role role) { this.role = role; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
