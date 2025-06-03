package com.cr.service;

import com.cr.dto.ConsultationRequestDTO;
import com.cr.entity.Consultation;
import com.cr.exception.ConsultationNotFoundException;
import com.cr.repository.ConsultationRepository;
import com.appointment.entity.Appointment;
import com.appointment.repository.AppointmentRepository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsultationService {

    @Autowired
    private ConsultationRepository consultationRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    public Consultation createConsultation(Long appointmentId, ConsultationRequestDTO consultationData) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new RuntimeException("Appointment not found with id " + appointmentId));
        
        // Ensure only one consultation per appointment
        if (consultationRepository.findByAppointment(appointment) != null) {
            throw new RuntimeException("A consultation record already exists for this appointment.");
        }
        Consultation consultation = new Consultation();
        
        //consultation.setConsultationId(appointmentId);
        Optional<Appointment> tempAppointment  = appointmentRepository.findById(appointmentId);
        consultation.setAppointment(tempAppointment.get());
        consultation.setNotes(consultationData.getNotes());
        consultation.setPrescription(consultationData.getPrescription());
        return consultationRepository.saveAndFlush(consultation);
    }
    
    public Consultation getConsultationByAppointmentId(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ConsultationNotFoundException("Appointment not found with id " + appointmentId));
        
        Consultation consultation = consultationRepository.findByAppointment(appointment);
        if (consultation == null) {
            // Throw our custom exception if consultation not found
            throw new ConsultationNotFoundException("Consultation not found for appointment id " + appointmentId);
        }
        return consultation;
    }
    
    public Consultation updateConsultation(Long consultationId, Consultation updatedData) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new RuntimeException("Consultation not found"));
        consultation.setNotes(updatedData.getNotes());
        consultation.setPrescription(updatedData.getPrescription());
        return consultationRepository.saveAndFlush(consultation);
    }

}

