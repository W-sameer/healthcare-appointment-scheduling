package com.dr.ava.repository;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dr.ava.entity.Availability;
import com.hospital.management.entity.User;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    Availability findByDoctorAndDate(User doctor, LocalDate date);
}

