package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.Doctor;
import com.example.demo.repository.DoctorRepository;
import com.doctor.availability.entity.Availability;
import com.doctor.availability.repository.AvailabilityRepository;
import com.doctor.availability.service.DoctorAvailabilityService;

@ExtendWith(MockitoExtension.class)
public class DoctorAvailabilityApplicationTests {

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorAvailabilityService doctorAvailabilityService;

    @Test
    void testSetAvailability_Success() {
        // Given
        Long doctorId = 1L;
        LocalDate date = LocalDate.of(2025, 6, 5);
        List<String> busySlots = List.of("10:00", "11:00");

        Doctor mockDoctor = new Doctor();
        mockDoctor.setDoctorId(doctorId);  // Assuming the entity uses setDoctorId

        Availability mockAvailability = new Availability();
        mockAvailability.setDoctor(mockDoctor);
        mockAvailability.setDate(date);
        mockAvailability.setBusySlots(List.of()); // Default empty slots

        // Mock repository behaviors
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(mockDoctor));
        when(availabilityRepository.findByDoctorAndDate(any(Doctor.class), eq(date))).thenReturn(mockAvailability);
        when(availabilityRepository.saveAndFlush(any(Availability.class))).thenReturn(mockAvailability);

        // When
        Availability result = doctorAvailabilityService.setAvailability(doctorId, date, busySlots);

        // Then
        assertNotNull(result);
        assertEquals(mockDoctor, result.getDoctor());
        assertEquals(date, result.getDate());
        assertEquals(busySlots.size(), result.getBusySlots().size());

        verify(doctorRepository, times(1)).findById(doctorId);
        verify(availabilityRepository, times(1)).findByDoctorAndDate(any(Doctor.class), eq(date));
        verify(availabilityRepository, times(1)).saveAndFlush(any(Availability.class));
    }
}
