// src/main/java/com/cr/service/MedicalHistoryService.java
package com.cr.service;

import com.cr.entity.MedicalHistory;

import java.util.List;

public interface MedicalHistoryService {
    MedicalHistory createMedicalHistory(MedicalHistory medicalHistory); // Accepts entity
    MedicalHistory getMedicalHistoryById(Long id);
    List<MedicalHistory> getMedicalHistoryByPatientId(Long patientId);
    MedicalHistory updateMedicalHistory(Long id, MedicalHistory medicalHistoryDetails); // Accepts entity for update
    void deleteMedicalHistory(Long id);
}