package com.dr.ava.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DoctorUnblockScheduleDTO {
    // The doctor's unique identifier.
    private Long doctorId;
    
    // The specific date for which the slots need to be unblocked.
    private LocalDate date;
    
    // A list of time slots (formatted as "HH:mm") that the doctor wishes to mark as available again.
    private List<String> unblockSlots;
}
