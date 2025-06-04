package com.doctor.availability.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointment.entity.Appointment;
import com.appointment.entity.AppointmentStatus;
import com.example.demo.entity.Doctor;
import com.example.demo.entity.User;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.UserRepository;
import com.appointment.repository.AppointmentRepository;
import com.appointment.service.AppointmentService;
import com.doctor.availability.dto.DoctorScheduleDTO;
import com.doctor.availability.entity.Availability;
import com.doctor.availability.repository.AvailabilityRepository;

@Service
public class DoctorAvailabilityService {

	@Autowired
	private AvailabilityRepository availabilityRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AppointmentRepository appointmentRepository;
	
    @Autowired
    private DoctorRepository doctorRepository;

	private final int SLOT_DURATION_MINUTES = 30;

	/**
	 * Set or update a doctor's availability for a given date. Typically, this
	 * endpoint is secured so that only authenticated doctors can update their own
	 * availability.
	 */
//    @Transactional
//    public Availability setAvailability(Long doctorId, LocalDate date, List<String> busySlots) {
//        User doctor = userRepository.findById(doctorId)
//                .orElseThrow(() -> new RuntimeException("Doctor not found"));
//        Availability availability = availabilityRepository.findByDoctorAndDate(doctor, date);
//        if (availability == null) {
//            availability = new Availability();
//            availability.setDoctor(doctor);
//            availability.setDate(date);
//        }
//        availability.setAvailableSlots(busySlots);
//        return availabilityRepository.saveAndFlush(availability);
//    }
//    

	@Transactional
	public Availability setAvailability(Long doctorId, LocalDate date, List<String> busySlots) {
		// Retrieve the doctor from the user repository.
		Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

		// Retrieve or create an Availability record for this doctor and date.
		Availability availability = availabilityRepository.findByDoctorAndDate(doctor, date);
		if (availability == null) {
			availability = new Availability();
			availability.setDoctor(doctor);
			availability.setDate(date);
		}

		// Convert busySlots (strings in "HH:mm") to LocalTime objects.
		List<LocalTime> blockedSlots = busySlots.stream().map(slotStr -> {
			try {
				return LocalTime.parse(slotStr);
			} catch (Exception e) {
				throw new RuntimeException("Invalid time format: " + slotStr);
			}
		})
				// Validate that the time falls within working hours and is not during lunch.
				.filter(slot -> {
					boolean withinWorkingHours = !slot.isBefore(WORK_START)
							&& !slot.plusMinutes(SLOT_DURATION_MINUTES).isAfter(WORK_END);
					boolean isDuringLunch = (!slot.isBefore(LUNCH_START) && slot.isBefore(LUNCH_END));
					return withinWorkingHours && !isDuringLunch;
				}).collect(Collectors.toList());

		// For each blocked slot, cancel any active appointment (even if booked, they
		// must be cancelled).
		for (LocalTime slot : blockedSlots) {
			// Retrieve appointments for the doctor at the given date and slot.
			List<Appointment> appointments = appointmentRepository.findByDoctorAndAppointmentDate(doctor, date).stream()
					.filter(app -> app.getAppointmentTime().equals(slot)
							&& app.getStatus() != AppointmentStatus.CANCELLED)
					.collect(Collectors.toList());
			// Cancel each appointment.
			for (Appointment app : appointments) {
				app.setStatus(AppointmentStatus.CANCELLED);
				appointmentRepository.save(app);
			}
		}

		// Save the blocked (busy) slots in the availability record.
		availability.setBusySlots(blockedSlots);

		// Optionally, update availableSlots here by computing the complement of the
		// default working slots.
		// For now, we assume availableSlots is handled elsewhere or computed
		// dynamically.

		// Persist the updated availability record.
		return availabilityRepository.saveAndFlush(availability);
	}

	/**
	 * Retrieve a doctor's availability for a specific date. This endpoint is public
	 * and does not require authentication – allowing patients to browse available
	 * slots.
	 */
	// Example constants (adjust if these are defined elsewhere in your project)

	public Availability getAvailability(Long doctorId, LocalDate date) {
		// Retrieve the doctor from the User repository.
		Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new RuntimeException("Doctor not found"));

		// Fetch all appointments for the specified doctor on the given date.
		List<Appointment> appointments = appointmentRepository.findByDoctorAndAppointmentDate(doctor, date);

		// Collect all time slots that have an active appointment (i.e. not CANCELLED).
		Set<LocalTime> bookedSlots = appointments.stream()
				.filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
				.map(Appointment::getAppointmentTime).collect(Collectors.toSet());

		// Generate the list of all potential appointment slots for the day,
		// excluding any slots that fall in the lunch break.
		List<LocalTime> allSlots = new ArrayList<>();
		LocalTime currentSlot = WORK_START;
		// Continue as long as the slot can fit into the working day.
		while (!currentSlot.plusMinutes(SLOT_DURATION_MINUTES).isAfter(WORK_END)) {
			// Add the slot if it is either before lunch or after the lunch break.
			if (currentSlot.isBefore(LUNCH_START) || currentSlot.isAfter(LUNCH_END) || currentSlot.equals(LUNCH_END)) {
				allSlots.add(currentSlot);
			}
			currentSlot = currentSlot.plusMinutes(SLOT_DURATION_MINUTES);
		}

		// Remove slots that have already been booked.
		List<LocalTime> computedAvailableSlots = allSlots.stream().filter(slot -> !bookedSlots.contains(slot))
				.collect(Collectors.toList());

		// Retrieve any existing availability record for the given doctor and date.
		Availability existingAvailability = availabilityRepository.findByDoctorAndDate(doctor, date);
		// If an availability record exists and busySlots are defined, use them; else,
		// use an empty list.
		List<LocalTime> busySlots = (existingAvailability != null && existingAvailability.getBusySlots() != null)
				? existingAvailability.getBusySlots()
				: new ArrayList<>();

		// Remove any manually blocked (busy) slots from the computed available slots.
		computedAvailableSlots.removeAll(busySlots);

		// Construct the final Availability object containing both fields.
		Availability result = new Availability();
		result.setDoctor(doctor);
		result.setDate(date);
		result.setAvailableSlots(computedAvailableSlots);
		result.setBusySlots(busySlots);

		return result;
	}

	/**
	 * Update the status of an appointment. When an appointment is completed or the
	 * patient misses the appointment, the doctor can mark it as COMPLETED or
	 * CANCELLED.
	 */
	@Transactional
	public Appointment updateAppointmentStatus(Long appointmentId, AppointmentStatus status) {
		Appointment appointment = appointmentRepository.findById(appointmentId)
				.orElseThrow(() -> new RuntimeException("Appointment not found"));
		appointment.setStatus(status);
		return appointmentRepository.saveAndFlush(appointment);
	}

	private final LocalTime WORK_START = LocalTime.of(9, 30);
	private final LocalTime WORK_END = LocalTime.of(18, 0);
	private final LocalTime LUNCH_START = LocalTime.of(13, 0);
	private final LocalTime LUNCH_END = LocalTime.of(14, 30);
	// FOLLOWUP_ALLOWED_START is not used in default free slot calculations
	private final LocalTime FOLLOWUP_ALLOWED_START = LocalTime.of(16, 0);

	// Used to fetch the doctor based on doctorId

	/**
	 * Sets the doctor's schedule for a specified date (which must be exactly the
	 * next day) using explicitly provided busy slots. All default working slots
	 * (30-minute intervals) are computed automatically and any slot the doctor
	 * marks as busy is removed.
	 *
	 * The resulting available slots are stored in the Availability entity.
	 */
	public Availability setDoctorSchedule(DoctorScheduleDTO scheduleDTO) {
		LocalDate scheduleDate = scheduleDTO.getDate();
		LocalDate expectedDate = LocalDate.now().plusDays(1);
		if (!scheduleDate.equals(expectedDate)) {
			throw new IllegalArgumentException("The schedule date must be exactly the next day: " + expectedDate);
		}

		// Compute default free slots as 30-minute intervals from WORK_START to
		// WORK_END, excluding the lunch period
		List<LocalTime> defaultSlots = new ArrayList<>();
		LocalTime slotTime = WORK_START;
		while (!slotTime.plusMinutes(30).isAfter(WORK_END)) {
			// Skip slots falling within the lunch break
			if (slotTime.compareTo(LUNCH_START) >= 0 && slotTime.isBefore(LUNCH_END)) {
				slotTime = slotTime.plusMinutes(30);
				continue;
			}
			defaultSlots.add(slotTime);
			slotTime = slotTime.plusMinutes(30);
		}

		// Parse the busy slots provided by the doctor (if any) into LocalTime objects
		List<LocalTime> busySlots = new ArrayList<>();
		if (scheduleDTO.getBusySlots() != null) {
			for (String busySlotStr : scheduleDTO.getBusySlots()) {
				busySlots.add(LocalTime.parse(busySlotStr));
			}
		}

		// Compute free slots by removing busy slots from default slots
		List<LocalTime> freeSlots = defaultSlots.stream().filter(slot -> !busySlots.contains(slot))
				.collect(Collectors.toList());

		// Retrieve the doctor's User entity using the provided doctorId
		Doctor doctor = doctorRepository.findById(scheduleDTO.getDoctorId()).orElseThrow(
				() -> new IllegalArgumentException("Doctor not found with id: " + scheduleDTO.getDoctorId()));

		// Build the Availability record
		Availability availability = new Availability();
		availability.setDoctor(doctor);
		availability.setDate(scheduleDate);
		availability.setAvailableSlots(freeSlots);

		// If you have an AvailabilityRepository, you can persist the record here, e.g.:
		// availability = availabilityRepository.save(availability);

		return availability;
	}
	
	@Transactional
	public Availability unblockAvailability(Long doctorId, LocalDate date, List<String> unblockSlots) {
	    // Retrieve the doctor from the user repository.
	    Doctor doctor = doctorRepository.findById(doctorId)
	            .orElseThrow(() -> new RuntimeException("Doctor not found"));
	    
	    // Retrieve the existing Availability record for this doctor and date.
	    Availability availability = availabilityRepository.findByDoctorAndDate(doctor, date);
	    if (availability == null) {
	        // If there's no record, you might choose to throw an error (or create a new one).
	        // Here, we'll throw an error for clarity.
	        throw new RuntimeException("Availability record not found for the given date");
	    }
	    
	    // Convert unblockSlots (strings in HH:mm) to LocalTime objects.
	    List<LocalTime> unblockLocalTimes = unblockSlots.stream()
	            .map(slotStr -> {
	                try {
	                    return LocalTime.parse(slotStr);
	                } catch (Exception e) {
	                    throw new RuntimeException("Invalid time format: " + slotStr);
	                }
	            })
	            // Validate that the time falls within working hours and is not during lunch.
	            .filter(slot -> {
	                boolean withinWorkingHours = !slot.isBefore(WORK_START) && 
	                        !slot.plusMinutes(SLOT_DURATION_MINUTES).isAfter(WORK_END);
	                boolean isDuringLunch = (!slot.isBefore(LUNCH_START) && slot.isBefore(LUNCH_END));
	                return withinWorkingHours && !isDuringLunch;
	            })
	            .collect(Collectors.toList());
	    
	    // Remove the unblocking slots from the current busySlots.
	    // If busySlots is null, initialize it to an empty list.
	    List<LocalTime> currentBusySlots = availability.getBusySlots();
	    if (currentBusySlots == null) {
	        currentBusySlots = new ArrayList<>();
	    }
	    
	    // Remove every time that the doctor is unblocking.
	    currentBusySlots.removeAll(unblockLocalTimes);
	    availability.setBusySlots(currentBusySlots);
	    
	    // Optionally, you might recalculate availableSlots
	    // e.g., using: availableSlots = defaultSlots - currentBusySlots - bookedSlots
	    
	    // Persist the updated availability record.
	    return availabilityRepository.saveAndFlush(availability);
	}
}
