package com.appointment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointment.dto.AppointmentRequestDTO;
import com.appointment.dto.AppointmentResponseDTO;
import com.appointment.dto.AppointmentUpdateDTO;
import com.appointment.dto.AvailabilityResponseDTO;
import com.appointment.dto.BookingResult;
import com.appointment.dto.DoctorResponseDTO;
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

	private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

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
	private final LocalTime WORK_END = LocalTime.of(18, 0);
	private final LocalTime LUNCH_START = LocalTime.of(13, 0);
	private final LocalTime LUNCH_END = LocalTime.of(14, 30);
	private final LocalTime FOLLOWUP_ALLOWED_START = LocalTime.of(16, 0);
	private final int SLOT_DURATION_MINUTES = 30;

	// Utility Validation Methods
	// Validate that a 30-minute slot fits completely within hospital hours.
	private void validateHospitalHours(LocalTime startTime) {
		if (startTime.isBefore(WORK_START) || startTime.plusMinutes(SLOT_DURATION_MINUTES).isAfter(WORK_END)) {
			throw new RuntimeException(
					"Appointment must be scheduled between 9:30 and 17:30 so that it finishes by 18:00.");
		}
	}

	// For regular(non-follow-up)appointments.
	private void validateRegularAppointment(LocalTime startTime) {

		if (startTime.isBefore(LUNCH_END) && startTime.plusMinutes(SLOT_DURATION_MINUTES).isAfter(LUNCH_START)) {
			throw new RuntimeException("Regular appointments cannot overlap the lunch break (13:00 to 14:30).");
		}
		if (!startTime.isBefore(FOLLOWUP_ALLOWED_START)) {
			logger.warn("Regular appointments not allowed between 16:00 and 18:00 at {}", startTime);
			throw new RuntimeException(
					"Regular appointments are not allowed between 16:00 and 18:00. For that period, select follow-up.");
		}
	}

	// Booking Methods
	@Transactional
	public BookingResult bookOrWaitAppointment(AppointmentRequestDTO request) {
		logger.info("Attempting to book appointment for patient {} with doctor {} on {} at {}", request.getPatientId(),
				request.getDoctorId(), request.getAppointmentDate(), request.getAppointmentTime());
		// Extract values from the DTO.
		Long patientId = request.getPatientId();
		Long doctorId = request.getDoctorId();
		LocalDate appointmentDate = request.getAppointmentDate();
		LocalTime appointmentTime = request.getAppointmentTime();
		LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);

		if (appointmentDateTime.isBefore(LocalDateTime.now())) {
			logger.error("Booking failed: Cannot book an appointment in the past.");
			throw new IllegalArgumentException("Cannot book an appointment in the past.");
		}
		BookingResult result = new BookingResult();

		// Retrieve patient and doctor using their dedicated repositories.
		Patient patient = patientRepository.findById(patientId)
				.orElseThrow(() -> new RuntimeException("Patient not found"));
		Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new RuntimeException("Doctor not found"));

		LocalDate date = appointmentDateTime.toLocalDate();
		LocalTime startTime = appointmentDateTime.toLocalTime();

		// Duplicate Check
		// Check if a booked appointment for the same doctor, patient, date, and time
		// exists.
		Appointment duplicateAppointment = appointmentRepository
				.findFirstByDoctorAndPatientAndAppointmentDateAndAppointmentTime(doctor, patient, date, startTime);
		if (duplicateAppointment != null) {
			logger.warn("Duplicate booking detected for patient {} with doctor {} on {} at {}", patientId, doctorId,
					date, startTime);
			throw new RuntimeException("Duplicate booking: an appointment for this slot already exists.");
		}

		// Check if the patient is already on the waiting list for the same doctor and
		// slot.
		List<WaitingAppointment> existingWaiting = waitingAppointmentRepository.findByDoctorAndPatient(doctor, patient);
		if (existingWaiting != null && !existingWaiting.isEmpty()) {
			boolean duplicateWaiting = existingWaiting.stream()
					.anyMatch(wait -> wait.getPreferredTime().toLocalDate().equals(date)
							&& wait.getPreferredTime().toLocalTime().equals(startTime));
			if (duplicateWaiting) {
				logger.warn("Duplicate waiting detected for patient {} with doctor {} at {}", patientId, doctorId,
						startTime);
				throw new RuntimeException("Duplicate waiting: You are already on the waiting list for this slot.");
			}
		}

		validateHospitalHours(startTime);
		validateRegularAppointment(startTime);

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
			logger.info("Slot available. Booking appointment for patient {} with doctor {} at {}", patientId, doctorId,
					startTime);
			// Create a new Appointment.
			Appointment appointment = new Appointment();
			appointment.setPatient(patient);
			appointment.setDoctor(doctor);
			appointment.setAppointmentDate(date);
			appointment.setAppointmentTime(startTime);
			appointment.setStatus(AppointmentStatus.BOOKED);
			// appointment.setFollowUp(followUp);
			Appointment saved = appointmentRepository.saveAndFlush(appointment);
			result.setBooked(true);
			result.setAppointment(saved);
			result.setMessage("Appointment booked successfully.");
			// Remove waiting records for this patient and doctor if any exist.
			List<WaitingAppointment> waitingRecords = waitingAppointmentRepository.findByDoctorAndPatient(doctor,
					patient);
			if (waitingRecords != null && !waitingRecords.isEmpty()) {
				waitingAppointmentRepository.deleteAll(waitingRecords);
				waitingAppointmentRepository.flush();
				logger.info("Removed waiting records for patient {} with doctor {}", patientId, doctorId);
			}
		} else {
			logger.warn("Requested slot is full. Adding patient {} to the waiting list for doctor {} at {}", patientId,
					doctorId, startTime);
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
		logger.info("Updating appointment {} with provided details", appointmentId);
		// 1. Retrieve the existing appointment.
		Appointment appointment = appointmentRepository.findById(appointmentId)
				.orElseThrow(() -> new RuntimeException("Appointment not found"));

		// Preserve the current (old) appointment slot.
		LocalDate oldDate = appointment.getAppointmentDate();
		LocalTime oldTime = appointment.getAppointmentTime();

		// 2. Update only the provided fields using explicit checks.
		if (updateDTO.getAppointmentDate() != null) {
			logger.debug("Updating appointment {} date from {} to {}", appointmentId, oldDate,
					updateDTO.getAppointmentDate());
			appointment.setAppointmentDate(updateDTO.getAppointmentDate());
		}
		if (updateDTO.getAppointmentTime() != null) {
			logger.debug("Updating appointment {} time from {} to {}", appointmentId, oldTime,
					updateDTO.getAppointmentTime());
			appointment.setAppointmentTime(updateDTO.getAppointmentTime());
		}

		// Determine the new slot from the updated appointment.
		LocalDate newDate = appointment.getAppointmentDate();
		LocalTime newTime = appointment.getAppointmentTime();
		LocalDateTime newDateTime = LocalDateTime.of(newDate, newTime);

		// 3. Validate the new date/time.
		if (newDateTime.isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("The appointment date/time cannot be in the past.");
		}

		validateHospitalHours(newTime);
		validateRegularAppointment(newTime);

		// 4. Save and flush the updated appointment.
		Appointment updatedAppointment = appointmentRepository.saveAndFlush(appointment);
		logger.info("Appointment {} updated successfully", appointmentId);

		// 5. If the slot has changed, then the old slot is now freed.
		if (!oldDate.equals(newDate) || !oldTime.equals(newTime)) {
			// The old slot is represented by oldDate + oldTime.
			LocalDateTime oldSlot = LocalDateTime.of(oldDate, oldTime);
			// Look for any waiting candidate for the exact same old slot.
			List<WaitingAppointment> waitingCandidates = waitingAppointmentRepository
					.findByDoctorAndPreferredTimeBetween(appointment.getDoctor(), oldSlot, oldSlot);
			if (waitingCandidates != null && !waitingCandidates.isEmpty()) {
				WaitingAppointment candidate = waitingCandidates.get(0);
				// Create a new appointment for the waiting candidate using the old (vacated)
				// slot.
				Appointment reassignedAppointment = new Appointment();
				reassignedAppointment.setDoctor(appointment.getDoctor());
				reassignedAppointment.setPatient(candidate.getPatient());
				reassignedAppointment.setAppointmentDate(oldDate);
				reassignedAppointment.setAppointmentTime(oldTime);
				reassignedAppointment.setStatus(AppointmentStatus.BOOKED);
				appointmentRepository.saveAndFlush(reassignedAppointment);
				// Remove the candidate from the waiting list.
				waitingAppointmentRepository.delete(candidate);
				logger.info("Reassigned old slot {} {} to waiting patient {}", oldDate, oldTime,
						candidate.getPatient().getUser().getName());
			}
		}

		return updatedAppointment;
	}

	// Convert Appointment entities to AppointmentResponseDTOs.
	public List<AppointmentResponseDTO> getAppointmentResponseDTOsByPatient(Long patientId) {
	    logger.info("Fetching appointment details for patient ID: {}", patientId);

	    List<Appointment> appointments = appointmentRepository.findByPatient_PatientId(patientId);

	    if (appointments.isEmpty()) {
	        logger.warn("No appointments found for patient ID: {}", patientId);
	    }

	    List<AppointmentResponseDTO> responseDTOs = appointments.stream()
	        .map(a -> AppointmentResponseDTO.builder()
	            .appointmentId(a.getAppointmentId())
	            .appointmentDate(a.getAppointmentDate())
	            .appointmentTime(a.getAppointmentTime())
	            .status(a.getStatus().name())
	            .doctor(
	                DoctorResponseDTO.builder()
	                    .doctorId(a.getDoctor().getDoctorId())
	                    .name(a.getDoctor().getUser() != null 
	                        ? a.getDoctor().getUser().getName() 
	                        : null)
	                    .gender(a.getDoctor().getUser() != null 
	                        ? a.getDoctor().getUser().getGender() 
	                        : null)
	                    .roomNumber(a.getDoctor().getRoomNumber())
	                    .specialization(a.getDoctor().getSpecialization())
	                    .qualification(a.getDoctor().getQualification())
	                    .build()
	            )
	            .build())
	        .collect(Collectors.toList());

	    logger.info("Successfully retrieved {} appointments for patient ID: {}", responseDTOs.size(), patientId);
	    return responseDTOs;
	}

	@Transactional
	public WaitingAppointment addToWaitingList(Long patientId, Long doctorId, LocalDateTime preferredTime) {
	    logger.info("Adding patient {} to waiting list for doctor {} at {}", patientId, doctorId, preferredTime);
		Patient patient = patientRepository.findById(patientId)
				.orElseThrow(() -> new RuntimeException("Patient not found"));
		Doctor doctor = doctorRepository.findById(doctorId)
				.orElseThrow(() -> new RuntimeException("Doctor not found"));

		if (!patient.getUser().getRole().equals(Role.PATIENT)) {
			 logger.warn("Invalid role: The provided patientId {} does not belong to a patient.", patientId);
			throw new RuntimeException("The provided patientId does not belong to a patient.");
		}
		if (!doctor.getUser().getRole().equals(Role.DOCTOR)) {
			logger.warn("Invalid role: The provided doctorId {} does not belong to a doctor.", doctorId);
			throw new RuntimeException("The provided doctorId does not belong to a doctor.");
		}

		WaitingAppointment waiting = new WaitingAppointment();
		waiting.setPatient(patient);
		waiting.setDoctor(doctor);
		waiting.setPreferredTime(preferredTime);

		WaitingAppointment savedWaiting = waitingAppointmentRepository.saveAndFlush(waiting);
	    logger.info("Successfully added patient {} to waiting list for doctor {} at {}", patientId, doctorId, preferredTime);

	    return savedWaiting;

	}
	@Transactional
	public Appointment cancelAppointment(Long appointmentId) {
		logger.info("Attempting to cancel appointment with ID: {}", appointmentId);
		// Fetch the appointment to be canceled.
		Appointment appointment = appointmentRepository.findById(appointmentId)
		        .orElseThrow(() -> {
		            logger.error("Appointment {} not found", appointmentId);
		            return new RuntimeException("Appointment not found");
		        });

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
			logger.info("Removed {} waiting records after appointment cancellation.", waitingList.size());
		}
		logger.info("Cancelled appointment {}. New status: {}", appointmentId, updatedAppointment.getStatus());
		return updatedAppointment;

	}

	public AvailabilityResponseDTO getAvailability(Long doctorId, LocalDate date) {
		 logger.info("Checking availability for doctor {} on {}", doctorId, date);
		AvailabilityResponseDTO response = new AvailabilityResponseDTO();
		response.setDoctorId(doctorId);
		response.setDate(date);
		response.setAvailableSlots(getAvailableTimeSlots(doctorId, date));
		logger.info("Availability check completed for doctor {} on {}: {} slots available", 
		        doctorId, date, response.getAvailableSlots().size());
		return response;

	}

	public List<LocalTime> getAvailableTimeSlots(Long doctorId, LocalDate date) {
	    logger.info("Fetching available time slots for doctor {} on {}", doctorId, date);
		Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new RuntimeException("Doctor not found"));

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
	    logger.info("Available slots for doctor {} on {}: {}", doctorId, date, slots);
		return slots;

	}

	@Transactional
	public void cancelOverdueAppointments() {
		LocalDate today = LocalDate.now();
		LocalTime currentTime = LocalTime.now();
		logger.info("Checking for overdue appointments on {}", today);

		List<Appointment> appointments = appointmentRepository.findByAppointmentDateAndStatus(today,
				AppointmentStatus.BOOKED);

		for (Appointment appointment : appointments) {
			if (appointment.getAppointmentTime().isBefore(currentTime)) {
				logger.info("Cancelling overdue appointment {} scheduled for {} {}", 
		                appointment.getAppointmentId(), appointment.getAppointmentDate(), appointment.getAppointmentTime());
				
				appointment.setStatus(AppointmentStatus.CANCELLED);
				appointmentRepository.save(appointment);

				LocalDateTime appointmentDateTime = appointment.getAppointmentDate()
						.atTime(appointment.getAppointmentTime());
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
		logger.info("Cancelled overdue appointments for {}", today);
	}
}
