// src/main/java/com/cr/entity/MedicalHistory.java
package com.cr.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull; // Added for validation
import jakarta.validation.constraints.PastOrPresent; // Added for validation
import jakarta.validation.constraints.Size; // Added for validation
import lombok.Data;

import java.time.LocalDate;

import com.example.demo.entity.Patient;

@Entity
@Table(name = "medical_history")
@Data
public class MedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long medicalHistoryId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient; // Represents the patient who owns this medical history

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "consultation_id")
//    private Consultation consultation;

//    @NotNull(message = "Entry date is required") // Validation directly on entity
//    @PastOrPresent(message = "Entry date cannot be in the future") // Validation directly on entity
//    private LocalDate entryDate;

    @Size(max = 1000, message = "Diagnosis cannot exceed 1000 characters") // Validation
    private String diagnosis;

//    @Column(columnDefinition = "TEXT")
//    @Size(max = 2000, message = "Notes cannot exceed 2000 characters") // Validation
//    private String notes;

    @Column(columnDefinition = "TEXT")
    @Size(max = 500, message = "Allergies cannot exceed 500 characters") // Validation
    private String allergies;

    @Column(columnDefinition = "TEXT")
    @Size(max = 500, message = "Chronic conditions cannot exceed 500 characters") // Validation
    private String breathingConditions;

//    @Column(columnDefinition = "TEXT")
//    @Size(max = 500, message = "Immunizations cannot exceed 500 characters") // Validation
//    private String immunizations;

    @Column(columnDefinition = "TEXT")
    @Size(max = 1000, message = "Past treatments cannot exceed 1000 characters") // Validation
    private String pastTreatments;

}