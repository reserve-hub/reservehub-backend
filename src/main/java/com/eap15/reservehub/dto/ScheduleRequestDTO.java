package com.eap15.reservehub.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ScheduleRequestDTO {

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalDateTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    @Future(message = "La hora de fin debe ser futura")
    private LocalDateTime endTime;

    @Min(value = 1, message = "Debe haber al menos un cupo disponible")
    private int availableSlots;

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(int availableSlots) { this.availableSlots = availableSlots; }
}
