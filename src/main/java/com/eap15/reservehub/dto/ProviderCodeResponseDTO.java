package com.eap15.reservehub.dto;

public class ProviderCodeResponseDTO {

    private Long id;
    private String code;
    private boolean used;
    private boolean active;

    public ProviderCodeResponseDTO() {}

    public ProviderCodeResponseDTO(Long id, String code, boolean used, boolean active) {
        this.id = id;
        this.code = code;
        this.used = used;
        this.active = active;
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
