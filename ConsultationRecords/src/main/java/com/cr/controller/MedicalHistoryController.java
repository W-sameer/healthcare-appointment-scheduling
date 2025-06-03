// src/main/java/com/cr/controller/MedicalHistoryController.java
package com.cr.controller;

import com.cr.entity.MedicalHistory;
import com.cr.service.MedicalHistoryService;
import jakarta.validation.Valid; // Still needed for validation on entity
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/medical-history")
public class MedicalHistoryController {

    @Autowired
    private MedicalHistoryService medicalHistoryService;

    // POST: Create a new medical history entry
    // Frontend should send patientId and consultationId as 'tempPatientId' and 'tempConsultationId' in the JSON body
    @PostMapping
    public ResponseEntity<MedicalHistory> createMedicalHistory(@Valid @RequestBody MedicalHistory medicalHistory) {
        MedicalHistory createdMedicalHistory = medicalHistoryService.createMedicalHistory(medicalHistory);
        return new ResponseEntity<>(createdMedicalHistory, HttpStatus.CREATED);
    }

    // GET: Retrieve a single medical history entry by ID
    @GetMapping("/{id}")
    public ResponseEntity<MedicalHistory> getMedicalHistoryById(@PathVariable Long id) {
        try {
            MedicalHistory medicalHistory = medicalHistoryService.getMedicalHistoryById(id);
            return ResponseEntity.ok(medicalHistory);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // GET: Retrieve all medical history entries for a specific patient
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalHistory>> getMedicalHistoryByPatient(@PathVariable Long patientId) {
        try {
            List<MedicalHistory> medicalHistories = medicalHistoryService.getMedicalHistoryByPatientId(patientId);
            return ResponseEntity.ok(medicalHistories);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // PUT: Update an existing medical history entry
    // Frontend sends the updated fields directly in the MedicalHistory object.
    // Be aware: any field not sent will be null, and your service must handle that.
    @PutMapping("/{id}")
    public ResponseEntity<MedicalHistory> updateMedicalHistory(@PathVariable Long id, @RequestBody MedicalHistory medicalHistoryDetails) {
        try {
            MedicalHistory updatedMedicalHistory = medicalHistoryService.updateMedicalHistory(id, medicalHistoryDetails);
            return ResponseEntity.ok(updatedMedicalHistory);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // DELETE: Delete a medical history entry
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalHistory(@PathVariable Long id) {
        try {
            medicalHistoryService.deleteMedicalHistory(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}