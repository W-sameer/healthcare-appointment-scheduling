// src/main/java/com/cr/repository/MedicalHistoryRepository.java
package com.cr.repository;

import com.cr.entity.Consultation;
import com.cr.entity.MedicalHistory;
import com.example.demo.entity.Patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    List<MedicalHistory> findByPatient_patientId(Long patientId);
   // Optional<MedicalHistory> findByConsultation_ConsultationId(Long consultationId);
	Optional<Consultation> findByPatient(Patient patient);
	Optional<MedicalHistory> findByMedicalHistoryId(Long id);
}