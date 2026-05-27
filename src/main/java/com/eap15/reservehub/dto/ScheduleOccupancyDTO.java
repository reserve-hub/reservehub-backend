package com.eap15.reservehub.dto;

import java.time.LocalDateTime;

/**
 * HU-12 Escenario 3 — Ocupación por franja horaria.
 * usedSlots + availableSlots = totalSlots (original capacity).
 */
public class ScheduleOccupancyDTO {

    private Long scheduleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalSlots;
    private int usedSlots;
    private int availableSlots;
    private double occupancyRate;  // porcentaje 0-100

    public ScheduleOccupancyDTO() {}

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }

    public int getUsedSlots() { return usedSlots; }
    public void setUsedSlots(int usedSlots) { this.usedSlots = usedSlots; }

    public int getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(int availableSlots) { this.availableSlots = availableSlots; }

    public double getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(double occupancyRate) { this.occupancyRate = occupancyRate; }
}
