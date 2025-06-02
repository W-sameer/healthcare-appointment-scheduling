package com.dr.ava.controller;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dr.ava.dto.AppointmentStatusUpdateDTO;
import com.dr.ava.dto.DoctorScheduleDTO;
import com.dr.ava.dto.DoctorUnblockScheduleDTO;
import com.dr.ava.entity.Availability;
import com.appschl.entity.Appointment;
import com.appschl.entity.AppointmentStatus;
import com.dr.ava.service.DoctorAvailabilityService;

@RestController
@RequestMapping("/api/availability")
public class DoctorAvailabilityController {

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
}

