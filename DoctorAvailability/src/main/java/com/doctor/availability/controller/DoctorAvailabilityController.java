package com.doctor.availability.controller;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.appointment.dto.AppointmentRequestDTO;
import com.appointment.dto.AppointmentUpdateDTO;
import com.appointment.entity.Appointment;
import com.appointment.entity.AppointmentStatus;
import com.appointment.service.AppointmentService;
import com.doctor.availability.dto.AppointmentStatusUpdateDTO;
import com.doctor.availability.dto.DoctorScheduleDTO;
import com.doctor.availability.dto.DoctorUnblockScheduleDTO;
import com.doctor.availability.entity.Availability;
import com.doctor.availability.service.DoctorAvailabilityService;

@RestController
@RequestMapping("/api/availability")
public class DoctorAvailabilityController {
	
	@Autowired
	private AppointmentService appointmentService;
	
    @Autowired
    private DoctorAvailabilityService availabilityService;
    
    /**
     * Public endpoint to retrieve a doctor's availability for a specified date.
     * Example: GET /api/availability?doctorId=10&date=2026-08-15
     */
    @GetMapping
    public ResponseEntity<Availability> getAvailability(
            @RequestParam Long doctorId, 
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        Availability availability = availabilityService.getAvailability(doctorId, localDate);
        return ResponseEntity.ok(availability);
    }
    
    /**
     * Protected endpoint for a doctor to set or update availability.
     * This endpoint is secured; only an authenticated doctor should be allowed to update their own availability.
     * Example: POST /api/availability with a JSON body containing doctor details, date, and available time slots.
     */
    @PostMapping("/setAvailability")
    public ResponseEntity<Availability> setAvailability(@RequestBody DoctorScheduleDTO doctorScheduleDTO) {
        Availability updated = availabilityService.setAvailability(
                doctorScheduleDTO.getDoctorId(),
                doctorScheduleDTO.getDate(),
                doctorScheduleDTO.getBusySlots());
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Protected endpoint to update the status of an appointment.
     * For example, after an appointment you may mark it as COMPLETED or CANCELLED.
     * Example: PUT /api/availability/appointments/42?status=COMPLETED
     */
    @PutMapping("/unblock")
    public ResponseEntity<Availability> unblockAvailabilitySlots(
            @RequestBody DoctorUnblockScheduleDTO doctorUnblockScheduleDTO) {
        
        Availability updated = availabilityService.unblockAvailability(
                doctorUnblockScheduleDTO.getDoctorId(),
                doctorUnblockScheduleDTO.getDate(),
                doctorUnblockScheduleDTO.getUnblockSlots());
        
        return ResponseEntity.ok(updated);
    }
    
    @PutMapping("/followup/{appointmentId}")
    public ResponseEntity<Appointment> createFollowUpAppointment(@PathVariable Long appointmentId, @RequestBody AppointmentUpdateDTO requestDTO) {
        // Force the followUp flag to true for follow-up appointments
        requestDTO.setFollowUp(requestDTO.getFollowUp());
        AppointmentUpdateDTO newRequestDTO  = new AppointmentUpdateDTO();
        newRequestDTO.setAppointmentDate(requestDTO.getAppointmentDate());
        newRequestDTO.setAppointmentTime(requestDTO.getAppointmentTime());
        newRequestDTO.setFollowUp(requestDTO.getFollowUp());
        
        // Delegate appointment creation to your existing service
        Appointment followUpAppointment = appointmentService.partialUpdateAppointment(appointmentId,requestDTO);
        return ResponseEntity.ok(followUpAppointment);
    }

}

