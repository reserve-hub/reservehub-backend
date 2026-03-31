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
    // Un codigo solo puede usarse UNA vez (HU-01: "no haya sido utilizado")
    @Column(nullable = false)
    private boolean used = false;

    public ProviderCode() {}

    public ProviderCode(String code) {
        this.code = code;
        this.used = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}