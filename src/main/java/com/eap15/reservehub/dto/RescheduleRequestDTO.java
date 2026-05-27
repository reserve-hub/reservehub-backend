package com.eap15.reservehub.dto;

import jakarta.validation.constraints.NotNull;

public class RescheduleRequestDTO {

    @NotNull(message = "El ID del nuevo horario es obligatorio")
    private Long newScheduleId;

    public RescheduleRequestDTO() {}

    public Long getNewScheduleId() { return newScheduleId; }
    public void setNewScheduleId(Long newScheduleId) { this.newScheduleId = newScheduleId; }
}
