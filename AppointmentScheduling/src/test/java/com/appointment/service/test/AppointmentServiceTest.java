package com.appointment.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import com.appointment.dto.AppointmentRequestDTO;
import com.appointment.dto.BookingResult;
import com.appointment.entity.Appointment;
import com.appointment.entity.AppointmentStatus;
import com.appointment.repository.AppointmentRepository;
import com.appointment.repository.WaitingAppointmentRepository;
import com.appointment.service.AppointmentService;
import com.example.demo.entity.Doctor;
import com.example.demo.entity.Patient;
import com.example.demo.entity.User;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.PatientRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @InjectMocks
    private AppointmentService appointmentService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private WaitingAppointmentRepository waitingAppointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Test
    public void testBookOrWaitAppointment_SuccessfulBooking() {
        // Arrange: Prepare the AppointmentRequestDTO with a valid, future appointment.
        AppointmentRequestDTO request = new AppointmentRequestDTO();
        request.setPatientId(1L);
        request.setDoctorId(7L);
        request.setAppointmentDate(LocalDate.of(2025, 7, 4));
        request.setAppointmentTime(LocalTime.of(10, 30));
      //  LocalDateTime appointmentDateTime = LocalDateTime.of(request.getAppointmentDate(), request.getAppointmentTime());

        // Create a dummy Patient.
        Patient patient = new Patient();
        patient.setPatientId(1L);

        // Create a dummy Doctor with an associated User.
        Doctor doctor = new Doctor();
        doctor.setDoctorId(7L);
        User doctorUser = new User();
        doctorUser.setUserId(14L);
        doctorUser.setName("Dr. Test");
        doctorUser.setGender("male");
        doctor.setUser(doctorUser);

        // Stub repository calls.
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(7L)).thenReturn(Optional.of(doctor));
        // Ensure no duplicate appointment exists.
        when(appointmentRepository.findFirstByDoctorAndPatientAndAppointmentDateAndAppointmentTime(
                eq(doctor), eq(patient), eq(request.getAppointmentDate()), eq(request.getAppointmentTime())))
                .thenReturn(null);
        // No waiting records.
        when(waitingAppointmentRepository.findByDoctorAndPatient(doctor, patient))
                .thenReturn(Collections.emptyList());
        // Simulate no overlapping existing appointments.
        when(appointmentRepository.findByDoctorAndAppointmentDate(eq(doctor), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Simulate saving the new appointment.
        Appointment savedAppointment = new Appointment();
        savedAppointment.setAppointmentId(15L);
        savedAppointment.setPatient(patient);
        savedAppointment.setDoctor(doctor);
        savedAppointment.setAppointmentDate(request.getAppointmentDate());
        savedAppointment.setAppointmentTime(request.getAppointmentTime());
        savedAppointment.setStatus(AppointmentStatus.BOOKED);
        savedAppointment.setFollowUp(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenReturn(savedAppointment);

        // Act: Call the service method.
        BookingResult result = appointmentService.bookOrWaitAppointment(request);

        // Assert:
        assertTrue(result.isBooked(), "Expected appointment to be booked.");
        assertNotNull(result.getAppointment(), "Expected non-null appointment in result.");
        assertEquals(15L, result.getAppointment().getAppointmentId().longValue(), "Appointment ID should match.");
        assertEquals("BOOKED", result.getAppointment().getStatus().name(), "Appointment status must be BOOKED.");
        assertEquals("Appointment booked successfully.", result.getMessage(), "Success message must be set.");
    }

    @Test
    public void testBookOrWaitAppointment_AppointmentInPastThrowsException() {
        // Arrange: Set up a request with an appointment date in the past.
        AppointmentRequestDTO request = new AppointmentRequestDTO();
        request.setPatientId(1L);
        request.setDoctorId(7L);
        request.setAppointmentDate(LocalDate.now().minusDays(1));
        request.setAppointmentTime(LocalTime.of(10, 30));

        // Act & Assert: Verify that booking an appointment in the past throws an exception.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.bookOrWaitAppointment(request);
        });
        assertEquals("Cannot book an appointment in the past.", exception.getMessage());
    }
}
