package com.eap15.reservehub.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-12 — Reporte operativo de reservas.
 * Usado tanto para el reporte de administrador como el de proveedor.
 * El campo {@code occupancy} solo se popula para proveedores (HU-12 Escenario 3).
 */
public class BookingReportDTO {

    private long total;
    private long confirmed;
    private long cancelled;
    private long rescheduled;

    /** Rango consultado; puede ser null si no se aplicó filtro de fechas. */
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    /** HU-12 Escenario 3 — ocupación por franja (solo en reporte de proveedor). */
    private List<ScheduleOccupancyDTO> occupancy;

    public BookingReportDTO() {}

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getConfirmed() { return confirmed; }
    public void setConfirmed(long confirmed) { this.confirmed = confirmed; }

    public long getCancelled() { return cancelled; }
    public void setCancelled(long cancelled) { this.cancelled = cancelled; }

    public long getRescheduled() { return rescheduled; }
    public void setRescheduled(long rescheduled) { this.rescheduled = rescheduled; }

    public LocalDateTime getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDateTime dateFrom) { this.dateFrom = dateFrom; }

    public LocalDateTime getDateTo() { return dateTo; }
    public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }

    public List<ScheduleOccupancyDTO> getOccupancy() { return occupancy; }
    public void setOccupancy(List<ScheduleOccupancyDTO> occupancy) { this.occupancy = occupancy; }
}
