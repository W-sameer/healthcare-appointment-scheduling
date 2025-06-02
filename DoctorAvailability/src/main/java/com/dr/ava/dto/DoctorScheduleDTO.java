package com.dr.ava.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DoctorScheduleDTO {
    private Long doctorId;
    private LocalDate date;         // Must be the next day
    // If the doctor explicitly marks some slots as busy, these will be removed from the default available slots.
    private List<String> busySlots; // e.g. ["11:00", "11:30", "15:00"]
}
