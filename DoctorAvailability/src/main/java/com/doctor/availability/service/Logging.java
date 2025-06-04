package com.doctor.availability.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import com.doctor.availability.entity.Availability;
import com.doctor.availability.repository.AvailabilityRepository;
import com.example.demo.entity.Doctor;
import com.example.demo.repository.DoctorRepository;

@Service
public class Logging {

    private static final Logger logger = LoggerFactory.getLogger(Logging.class);

    private final AvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;

    public Logging(AvailabilityRepository availabilityRepository, DoctorRepository doctorRepository) {
        this.availabilityRepository = availabilityRepository;
        this.doctorRepository = doctorRepository;
    }

    public Availability getAvailability(Long doctorId, LocalDate date) {
        logger.debug("Fetching availability for doctorId: {} on date: {}", doctorId, date);
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Availability availability = availabilityRepository.findByDoctorAndDate(doctor, date);
        if (availability == null) {
            logger.info("No previous availability found for doctorId: {} on date: {}. Creating a new one.", doctorId, date);
            availability = new Availability();
            availability.setDoctor(doctor);
            availability.setDate(date);
        }

        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            logger.info("Date {} is a Sunday. Doctor is on leave; clearing available slots.", date);
            availability.setAvailableSlots(Collections.emptyList());
        }
        return availability;
    }

    @Transactional
    public Availability setAvailability(Long doctorId, LocalDate date, List<LocalTime> busySlots) {
        logger.debug("Setting availability for doctorId: {} on date: {} with busySlots: {}", doctorId, date, busySlots);
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Availability availability = availabilityRepository.findByDoctorAndDate(doctor, date);
        if (availability == null) {
            logger.info("Creating new availability record for doctorId: {} on date: {}", doctorId, date);
            availability = new Availability();
            availability.setDoctor(doctor);
            availability.setDate(date);
        }

        availability.setBusySlots(busySlots);

        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            logger.warn("Attempting to set availability on Sunday. Clearing available slots for doctorId: {} on date: {}", doctorId, date);
            availability.setAvailableSlots(Collections.emptyList());
        }
        return availabilityRepository.saveAndFlush(availability);
    }

    @Transactional
    public Availability unblockAvailability(Long doctorId, LocalDate date, List<String> unblockSlots) {
        logger.debug("Unblocking slots {} for doctorId: {} on date: {}", unblockSlots, doctorId, date);
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Availability availability = availabilityRepository.findByDoctorAndDate(doctor, date);
        if (availability == null) {
            throw new RuntimeException("Availability record not found for doctor and date");
        }

        List<LocalTime> currentBusySlots = availability.getBusySlots();
        currentBusySlots.removeAll(unblockSlots);
        availability.setBusySlots(currentBusySlots);

        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            logger.info("Date {} is Sunday; ensuring available slots remain empty.", date);
            availability.setAvailableSlots(Collections.emptyList());
        }

        return availabilityRepository.saveAndFlush(availability);
    }
}

