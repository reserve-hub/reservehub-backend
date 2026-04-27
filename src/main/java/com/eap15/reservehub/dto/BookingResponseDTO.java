package com.eap15.reservehub.dto;

import com.eap15.reservehub.entity.Booking;

import java.time.LocalDateTime;

public class BookingResponseDTO {

    private Long id;
    private Long clientId;
    private String clientName;
    private Long scheduleId;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private String providerName;
    private String serviceType;
    private Booking.BookingStatus status;
    private LocalDateTime createdAt;

    public BookingResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public LocalDateTime getScheduleStartTime() { return scheduleStartTime; }
    public void setScheduleStartTime(LocalDateTime scheduleStartTime) { this.scheduleStartTime = scheduleStartTime; }

    public LocalDateTime getScheduleEndTime() { return scheduleEndTime; }
    public void setScheduleEndTime(LocalDateTime scheduleEndTime) { this.scheduleEndTime = scheduleEndTime; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public Booking.BookingStatus getStatus() { return status; }
    public void setStatus(Booking.BookingStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
