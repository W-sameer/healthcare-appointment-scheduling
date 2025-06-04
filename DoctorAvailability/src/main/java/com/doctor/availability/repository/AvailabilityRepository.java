package com.doctor.availability.repository;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.doctor.availability.entity.Availability;
import com.example.demo.entity.Doctor;
import com.example.demo.entity.User;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

	Availability findByDoctorAndDate(Doctor doctor, LocalDate date);
}

