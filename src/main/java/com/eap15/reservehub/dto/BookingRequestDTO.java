package com.eap15.reservehub.dto;

import jakarta.validation.constraints.NotNull;

public class BookingRequestDTO {

    @NotNull(message = "El ID de la franja horaria es obligatorio")
    private Long scheduleId;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
}
