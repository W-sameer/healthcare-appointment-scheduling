//package com.example.demo.controller;
////package com.cr.controller;
//
//import com.cr.controller.ConsultationController;
//import com.cr.dto.ConsultationRequestDTO;
//import com.cr.entity.Consultation;
//import com.cr.exception.ConsultationNotFoundException;
//import com.cr.service.ConsultationService;
//import com.example.demo.config.JwtUtil;
//import com.appointment.entity.Appointment; // Assuming this path
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
////@WebMvcTest(ConsultationController.class)
//@SpringBootTest
//public class ConsultationControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//    @MockBean
//    private JwtUtil jwtUtil;
//
//    @MockBean
//    private ConsultationService consultationService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private Consultation mockConsultation;
//    private ConsultationRequestDTO mockConsultationRequestDTO;
//    private Appointment mockAppointment;
//
//    @BeforeEach
//    void setUp() {
//        mockAppointment = new Appointment();
//        mockAppointment.setAppointmentId(1L);
//        // Set other necessary appointment fields if needed for full object comparison
//
//        mockConsultation = new Consultation();
//        mockConsultation.setConsultationId(101L);
//        mockConsultation.setAppointment(mockAppointment);
//        mockConsultation.setNotes("Initial notes for consultation");
//        Map<String, String> prescription = new HashMap<>();
//        prescription.put("MedicineA", "10mg daily");
//        mockConsultation.setPrescription(prescription);
//
//        mockConsultationRequestDTO = new ConsultationRequestDTO(null, prescription);
//        mockConsultationRequestDTO.setNotes("New consultation notes");
//        Map<String, String> requestPrescription = new HashMap<>();
//        requestPrescription.put("MedicineB", "20mg daily");
//        mockConsultationRequestDTO.setPrescription(requestPrescription);
//    }
//
//    @Test
//    void createConsultation_shouldReturnCreatedConsultation() throws Exception {
//        when(consultationService.createConsultation(eq(1L), any(ConsultationRequestDTO.class)))
//                .thenReturn(mockConsultation);
//
//        mockMvc.perform(post("/api/consultations/{appointmentId}", 1L)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(mockConsultationRequestDTO)))
//                .andExpect(status().isOk()) // Assuming 200 OK as per your controller
//                .andExpect(jsonPath("$.consultationId").value(101L))
//                .andExpect(jsonPath("$.notes").value("Initial notes for consultation"))
//                .andExpect(jsonPath("$.prescription.MedicineA").value("10mg daily"));
//    }
//
//    @Test
//    void getConsultation_shouldReturnConsultation() throws Exception {
//        when(consultationService.getConsultationByAppointmentId(1L))
//                .thenReturn(mockConsultation);
//
//        mockMvc.perform(get("/api/consultations/{appointmentId}", 1L))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.consultationId").value(101L))
//                .andExpect(jsonPath("$.notes").value("Initial notes for consultation"));
//    }
//
//    @Test
//    void getConsultation_shouldReturnNotFound_whenConsultationDoesNotExist() throws Exception {
//        when(consultationService.getConsultationByAppointmentId(99L))
//                .thenThrow(new ConsultationNotFoundException("Consultation not found"));
//
//        mockMvc.perform(get("/api/consultations/{appointmentId}", 99L))
//                .andExpect(status().isNotFound()); // Expecting 404 due to ConsultationNotFoundException
//    }
//
//    @Test
//    void updateConsultation_shouldReturnUpdatedConsultation() throws Exception {
//        Consultation updatedConsultationData = new Consultation();
//        updatedConsultationData.setNotes("Updated notes for consultation");
//        Map<String, String> updatedPrescription = new HashMap<>();
//        updatedPrescription.put("MedicineC", "30mg daily");
//        updatedConsultationData.setPrescription(updatedPrescription);
//
//        Consultation returnedUpdatedConsultation = new Consultation();
//        returnedUpdatedConsultation.setConsultationId(101L);
//        returnedUpdatedConsultation.setNotes("Updated notes for consultation");
//        returnedUpdatedConsultation.setPrescription(updatedPrescription);
//
//
//        when(consultationService.updateConsultation(eq(101L), any(Consultation.class)))
//                .thenReturn(returnedUpdatedConsultation);
//
//        mockMvc.perform(put("/api/consultations/{consultationId}", 101L)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(updatedConsultationData)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.consultationId").value(101L))
//                .andExpect(jsonPath("$.notes").value("Updated notes for consultation"))
//                .andExpect(jsonPath("$.prescription.MedicineC").value("30mg daily"));
//    }
//
//    @Test
//    void updateConsultation_shouldReturnNotFound_whenConsultationDoesNotExist() throws Exception {
//        Consultation updatedConsultationData = new Consultation();
//        updatedConsultationData.setNotes("Non-existent notes");
//
//        when(consultationService.updateConsultation(eq(999L), any(Consultation.class)))
//                .thenThrow(new RuntimeException("Consultation not found")); // As per your service
//
//        mockMvc.perform(put("/api/consultations/{consultationId}", 999L)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(updatedConsultationData)))
//                .andExpect(status().isInternalServerError()); // RuntimeException often maps to 500 by default
//    }
//}
package com.example.demo.controller;

import com.cr.controller.ConsultationController;
import com.cr.dto.ConsultationRequestDTO;
import com.cr.entity.Consultation;
import com.cr.exception.ConsultationNotFoundException;
import com.cr.service.ConsultationService;
import com.example.demo.config.JwtUtil; // Ensure correct package for JwtUtil
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsultationController.class)
public class ConsultationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultationService consultationService;

    @MockBean
    private JwtUtil jwtUtil; // Mock JwtUtil to avoid missing bean issues

    @Autowired
    private ObjectMapper objectMapper;

    private Consultation mockConsultation;
    private ConsultationRequestDTO mockConsultationRequestDTO;

    @BeforeEach
    void setUp() {
        mockConsultation = new Consultation();
        mockConsultation.setConsultationId(101L);
        mockConsultation.setNotes("Initial notes for consultation");
        Map<String, String> prescription = new HashMap<>();
        prescription.put("MedicineA", "10mg daily");
        mockConsultation.setPrescription(prescription);

        mockConsultationRequestDTO = new ConsultationRequestDTO(null, prescription);
        mockConsultationRequestDTO.setNotes("New consultation notes");
        Map<String, String> requestPrescription = new HashMap<>();
        requestPrescription.put("MedicineB", "20mg daily");
        mockConsultationRequestDTO.setPrescription(requestPrescription);
    }

    @Test
    void createConsultation_shouldReturnCreatedConsultation() throws Exception {
        when(consultationService.createConsultation(eq(1L), any(ConsultationRequestDTO.class)))
                .thenReturn(mockConsultation);

        mockMvc.perform(post("/api/consultations/{appointmentId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockConsultationRequestDTO)))
                .andExpect(status().isOk()) // Assuming 200 OK as per your controller
                .andExpect(jsonPath("$.consultationId").value(101L))
                .andExpect(jsonPath("$.notes").value("Initial notes for consultation"))
                .andExpect(jsonPath("$.prescription.MedicineA").value("10mg daily"));
    }

    @Test
    void getConsultation_shouldReturnConsultation() throws Exception {
        when(consultationService.getConsultationByAppointmentId(1L))
                .thenReturn(mockConsultation);

        mockMvc.perform(get("/api/consultations/{appointmentId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultationId").value(101L))
                .andExpect(jsonPath("$.notes").value("Initial notes for consultation"));
    }

    @Test
    void getConsultation_shouldReturnNotFound_whenConsultationDoesNotExist() throws Exception {
        when(consultationService.getConsultationByAppointmentId(99L))
                .thenThrow(new ConsultationNotFoundException("Consultation not found"));

        mockMvc.perform(get("/api/consultations/{appointmentId}", 99L))
                .andExpect(status().isNotFound()); // Expecting 404 due to ConsultationNotFoundException
    }

    @Test
    void updateConsultation_shouldReturnUpdatedConsultation() throws Exception {
        Consultation updatedConsultationData = new Consultation();
        updatedConsultationData.setNotes("Updated notes for consultation");
        Map<String, String> updatedPrescription = new HashMap<>();
        updatedPrescription.put("MedicineC", "30mg daily");
        updatedConsultationData.setPrescription(updatedPrescription);

        Consultation returnedUpdatedConsultation = new Consultation();
        returnedUpdatedConsultation.setConsultationId(101L);
        returnedUpdatedConsultation.setNotes("Updated notes for consultation");
        returnedUpdatedConsultation.setPrescription(updatedPrescription);

        when(consultationService.updateConsultation(eq(101L), any(Consultation.class)))
                .thenReturn(returnedUpdatedConsultation);

        mockMvc.perform(put("/api/consultations/{consultationId}", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedConsultationData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultationId").value(101L))
                .andExpect(jsonPath("$.notes").value("Updated notes for consultation"))
                .andExpect(jsonPath("$.prescription.MedicineC").value("30mg daily"));
    }

    @Test
    void updateConsultation_shouldReturnNotFound_whenConsultationDoesNotExist() throws Exception {
        Consultation updatedConsultationData = new Consultation();
        updatedConsultationData.setNotes("Non-existent notes");

        when(consultationService.updateConsultation(eq(999L), any(Consultation.class)))
                .thenThrow(new RuntimeException("Consultation not found"));

        mockMvc.perform(put("/api/consultations/{consultationId}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedConsultationData)))
                .andExpect(status().isInternalServerError()); // Maps RuntimeException to 500 by default
    }
}
