package com.cr.dto;

import com.appointment.entity.Appointment;
import jakarta.persistence.*;
import lombok.*;
import java.util.Map;
import java.util.HashMap;

@AllArgsConstructor
@Data
public class ConsultationRequestDTO {
    private String notes;
    private Map<String, String> prescription = new HashMap<>();
}
