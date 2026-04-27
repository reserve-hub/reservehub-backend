package com.eap15.reservehub.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "provider_codes")
public class ProviderCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El codigo en si: ej "PROV-ABC123-XYZ"
    // unique=true: no pueden existir dos codigos iguales
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    // false = disponible para usar / true = ya fue utilizado
    @Column(nullable = false)
    private boolean used = false;

    // true = activo (puede usarse) / false = desactivado por admin (HU-09)
    @Column(nullable = false)
    private boolean active = true;

    public ProviderCode() {}

    public ProviderCode(String code) {
        this.code = code;
        this.used = false;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}