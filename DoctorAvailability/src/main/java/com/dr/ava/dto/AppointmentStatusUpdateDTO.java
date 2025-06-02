package com.dr.ava.dto;

import com.appschl.entity.AppointmentStatus;
import lombok.Data;

@Data
public class AppointmentStatusUpdateDTO {
    // The ID of the appointment to update
    private Long appointmentId;
    // The new status to apply to the appointment
    private AppointmentStatus status;
}
