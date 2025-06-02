package com.appointment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointment.dto.AppointmentUpdateDTO;
import com.appointment.dto.AvailabilityResponseDTO;
import com.appointment.dto.BookingResult;
import com.appointment.entity.Appointment;
import com.appointment.entity.AppointmentStatus;
import com.appointment.entity.WaitingAppointment;
import com.appointment.repository.AppointmentRepository;
import com.appointment.repository.WaitingAppointmentRepository;
import com.example.demo.entity.Role;
import com.example.demo.entity.Doctor;
import com.example.demo.entity.Patient;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.PatientRepository;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private WaitingAppointmentRepository waitingAppointmentRepository;
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    // Hospital working hours and breaks.
    private final LocalTime WORK_START = LocalTime.of(9, 30);
    private final LocalTime WORK_END   = LocalTime.of(18, 0);
    private final LocalTime LUNCH_START = LocalTime.of(13, 0);
    private final LocalTime LUNCH_END   = LocalTime.of(14, 30);
    private final LocalTime FOLLOWUP_ALLOWED_START = LocalTime.of(16, 0);
    private static final int SLOT_DURATION_MINUTES = 30;
    
    // --- Utility Validation Methods ---
    // Validate that a 30-minute slot fits completely within hospital hours.
    private void validateHospitalHours(LocalTime startTime) {
        if (startTime.isBefore(WORK_START) || startTime.plusMinutes(SLOT_DURATION_MINUTES).isAfter(WORK_END)) {
            throw new RuntimeException("Appointment must be scheduled between 9:30 and 17:30 so that it finishes by 18:00.");
        }
    }
    
    // For regular (non-follow-up) appointments.
    private void validateRegularAppointment(LocalTime startTime) {
        if (startTime.isBefore(LUNCH_END) && startTime.plusMinutes(SLOT_DURATION_MINUTES).isAfter(LUNCH_START)) {
            throw new RuntimeException("Regular appointments cannot overlap the lunch break (13:00 to 14:30).");
        }
        if (!startTime.isBefore(FOLLOWUP_ALLOWED_START)) {
            throw new RuntimeException("Regular appointments are not allowed between 16:00 and 18:00. For that period, select follow-up.");
        }
    }
    
    // For follow-up appointments:
    private void validateFollowUpAppointment(LocalTime startTime) {
        if (startTime.isBefore(FOLLOWUP_ALLOWED_START) || startTime.plusMinutes(SLOT_DURATION_MINUTES).isAfter(WORK_END)) {
            throw new RuntimeException("Follow-up appointments must be scheduled between 16:00 and 18:00.");
        }
    }
    
    // --- Booking Methods ---
    @Transactional
    public BookingResult bookOrWaitAppointment(Long patientId, Long doctorId, LocalDateTime appointmentDateTime, boolean followUp) {
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book an appointment in the past.");
        }
        BookingResult result = new BookingResult();
        
        // Retrieve patient and doctor using their dedicated repositories.
        Patient patient = patientRepository.findById(patientId)
        	    .orElseThrow(() -> new RuntimeException("Patient not found"));
        	Doctor doctor = doctorRepository.findById(doctorId)
        	    .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // Optional: Validate that the retrieved entities hold the proper roles.
        if (!patient.getUser().getRole().equals(Role.PATIENT)) {
            throw new RuntimeException("The provided patientId does not belong to a patient.");
        }
        if (!doctor.getUser().getRole().equals(Role.DOCTOR)) {
            throw new RuntimeException("The provided doctorId does not belong to a doctor.");
        }
        
        LocalDate date = appointmentDateTime.toLocalDate();
        LocalTime startTime = appointmentDateTime.toLocalTime();

        // ----- Duplicate Check -----
        // Check if a booked appointment for the same doctor, patient, date, and time exists.
        Appointment duplicateAppointment = appointmentRepository
                .findFirstByDoctorAndPatientAndAppointmentDateAndAppointmentTime(doctor, patient, date, startTime);
        if (duplicateAppointment != null) {
            throw new RuntimeException("Duplicate booking: an appointment for this slot already exists.");
        }

        // Check if the patient is already on the waiting list for the same doctor and slot.
        List<WaitingAppointment> existingWaiting = waitingAppointmentRepository.findByDoctorAndPatient(doctor, patient);
        if (existingWaiting != null && !existingWaiting.isEmpty()) {
            boolean duplicateWaiting = existingWaiting.stream().anyMatch(wait ->
                wait.getPreferredTime().toLocalDate().equals(date) &&
                wait.getPreferredTime().toLocalTime().equals(startTime));
            if (duplicateWaiting) {
                throw new RuntimeException("Duplicate waiting: You are already on the waiting list for this slot.");
            }
        }
        
        // ----- Validate appointment timing -----
        validateHospitalHours(startTime);
        if (followUp) {
            validateFollowUpAppointment(startTime);
        } else {
            validateRegularAppointment(startTime);
        }
        
        LocalTime endTime = startTime.plusMinutes(SLOT_DURATION_MINUTES);
        List<Appointment> existingAppointments = appointmentRepository.findByDoctorAndAppointmentDate(doctor, date);
        boolean slotAvailable = true;
        // Check for overlaps with existing appointments.
        for (Appointment a : existingAppointments) {
            LocalTime exStart = a.getAppointmentTime();
            LocalTime exEnd = exStart.plusMinutes(SLOT_DURATION_MINUTES);
            if (startTime.isBefore(exEnd) && exStart.isBefore(endTime)) {
                slotAvailable = false;
                break;
            }
        }
        if (slotAvailable) {
            // Create a new Appointment.
            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setDoctor(doctor);
            appointment.setAppointmentDate(date);
            appointment.setAppointmentTime(startTime);
            appointment.setStatus(AppointmentStatus.BOOKED);
            appointment.setFollowUp(followUp);
            Appointment saved = appointmentRepository.saveAndFlush(appointment);
            result.setBooked(true);
            result.setAppointment(saved);
            result.setMessage("Appointment booked successfully.");
            // Remove waiting records for this patient and doctor if any exist.
            List<WaitingAppointment> waitingRecords = waitingAppointmentRepository.findByDoctorAndPatient(doctor, patient);
            if (waitingRecords != null && !waitingRecords.isEmpty()) {
                waitingAppointmentRepository.deleteAll(waitingRecords);
                waitingAppointmentRepository.flush();
            }
        } else {
            // Slot unavailable: add patient to waiting list.
            WaitingAppointment waiting = addToWaitingList(patientId, doctorId, appointmentDateTime);
            result.setBooked(false);
            result.setWaitingAppointment(waiting);
            result.setMessage("Requested slot is full. You have been added to the waiting list automatically.");
        }
        
        return result;
    }
    
    @Transactional
    public Appointment partialUpdateAppointment(Long appointmentId, AppointmentUpdateDTO updateDTO) {
        // Retrieve the existing appointment.
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Determine the new appointment date.
        LocalDate newDate = updateDTO.getAppointmentDate() != null 
                ? updateDTO.getAppointmentDate() 
                : appointment.getAppointmentDate();

        // Determine the new appointment time.
        LocalTime newTime = updateDTO.getAppointmentTime() != null 
                ? updateDTO.getAppointmentTime() 
                : appointment.getAppointmentTime();

        // Construct the new appointment LocalDateTime.
        LocalDateTime newDateTime = LocalDateTime.of(newDate, newTime);

        // Call the helper method to validate this new date/time.
        validateAppointmentDateTime(newDateTime);

        // If validation passes, update the fields.
        if (updateDTO.getAppointmentDate() != null) {
            appointment.setAppointmentDate(updateDTO.getAppointmentDate());
        }
        if (updateDTO.getAppointmentTime() != null) {
            appointment.setAppointmentTime(updateDTO.getAppointmentTime());
        }
        if (updateDTO.getFollowUp() != null) {
            appointment.setFollowUp(updateDTO.getFollowUp());
        }

        // Save and flush the updated appointment.
        return appointmentRepository.saveAndFlush(appointment);
    }

    /**
     * Validates that the given appointment date/time is in the future.
     * Throws an IllegalArgumentException if the date/time is invalid.
     */
    private void validateAppointmentDateTime(LocalDateTime dateTime) {
        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The appointment date/time cannot be in the past.");
        }
    }

    
    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        // Adjust based on your naming conventions (assumes Appointment entity has a Patient field with patientId).
        return appointmentRepository.findByPatient_PatientId(patientId);
    }
    
    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctor_DoctorId(doctorId);
    }
    
    @Transactional
    public WaitingAppointment addToWaitingList(Long patientId, Long doctorId, LocalDateTime preferredTime) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        if (!patient.getUser().getRole().equals(Role.PATIENT)) {
            throw new RuntimeException("The provided patientId does not belong to a patient.");
        }
        if (!doctor.getUser().getRole().equals(Role.DOCTOR)) {
            throw new RuntimeException("The provided doctorId does not belong to a doctor.");
        }
        
        WaitingAppointment waiting = new WaitingAppointment();
        waiting.setPatient(patient);
        waiting.setDoctor(doctor);
        waiting.setPreferredTime(preferredTime);
        
        return waitingAppointmentRepository.saveAndFlush(waiting);
    }
    
    @Transactional
    public Appointment cancelAppointment(Long appointmentId) {
        // Fetch the appointment to be canceled.
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
    
        // Build a LocalDateTime for the appointment and define a ±15-minute window.
        LocalDateTime apptDateTime = appointment.getAppointmentDate().atTime(appointment.getAppointmentTime());
        LocalDateTime startWindow = apptDateTime.minusMinutes(15);
        LocalDateTime endWindow = apptDateTime.plusMinutes(15);
    
        // Look for waiting appointments for the same doctor within this window.
        List<WaitingAppointment> waitingList = waitingAppointmentRepository
                .findByDoctorAndPreferredTimeBetween(appointment.getDoctor(), startWindow, endWindow);
        
        // If there are waiting appointments, reassign the appointment.
        if (waitingList != null && !waitingList.isEmpty()) {
            WaitingAppointment waiting = waitingList.get(0);
            appointment.setPatient(waiting.getPatient());
            appointment.setStatus(AppointmentStatus.BOOKED);
        } else {
            appointment.setStatus(AppointmentStatus.CANCELLED);
        }
    
        Appointment updatedAppointment = appointmentRepository.save(appointment);
    
        // Cleanup: remove waiting records if present.
        if (waitingList != null && !waitingList.isEmpty()) {
            waitingAppointmentRepository.deleteAll(waitingList);
            waitingAppointmentRepository.flush();
        }
        return updatedAppointment;
    }
    
    public List<LocalTime> getAvailableTimeSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        List<LocalTime> slots = new ArrayList<>();
        LocalTime slotTime = WORK_START;
        while (!slotTime.plusMinutes(30).isAfter(WORK_END)) {
            // Exclude slots overlapping lunch.
            if (!(slotTime.isBefore(LUNCH_END) && slotTime.plusMinutes(30).isAfter(LUNCH_START))) {
                slots.add(slotTime);
            }
            slotTime = slotTime.plusMinutes(30);
        }
        
        List<Appointment> appointments = appointmentRepository.findByDoctorAndAppointmentDate(doctor, date);
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.BOOKED) {
                slots.remove(a.getAppointmentTime());
            }
        }
        
        return slots;
    }
    
    public AvailabilityResponseDTO getAvailability(Long doctorId, LocalDate date) {
        AvailabilityResponseDTO response = new AvailabilityResponseDTO();
        response.setDoctorId(doctorId);
        response.setDate(date);
        response.setAvailableSlots(getAvailableTimeSlots(doctorId, date));
        return response;
    }
    
    @Transactional
    public void cancelOverdueAppointments() {
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
    
        List<Appointment> appointments = appointmentRepository.findByAppointmentDateAndStatus(today, AppointmentStatus.BOOKED);
    
        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentTime().isBefore(currentTime)) {
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(appointment);
    
                LocalDateTime appointmentDateTime = appointment.getAppointmentDate().atTime(appointment.getAppointmentTime());
                LocalDateTime startWindow = appointmentDateTime.minusMinutes(15);
                LocalDateTime endWindow = appointmentDateTime.plusMinutes(15);
    
                List<WaitingAppointment> waitingList = waitingAppointmentRepository
                        .findByDoctorAndPreferredTimeBetween(appointment.getDoctor(), startWindow, endWindow);
    
                if (waitingList != null && !waitingList.isEmpty()) {
                    waitingAppointmentRepository.deleteAll(waitingList);
                    waitingAppointmentRepository.flush();
                }
            }
        }
    }
}
