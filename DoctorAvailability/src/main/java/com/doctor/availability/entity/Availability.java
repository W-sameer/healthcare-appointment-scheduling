package com.doctor.availability.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.demo.entity.Doctor;
import com.example.demo.entity.User;

@Entity
@Table(name = "availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each availability record is associated with a doctor.
    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    // The date for which these slots apply.
    @Column(nullable = false)
    private LocalDate date;

    // A list of time slots when the doctor is available for appointments.
    @ElementCollection
    //@CollectionTable(name = "availability_time_slots", joinColumns = @JoinColumn(name = "availability_id"))
    @Column(name = "time_slot")
    private List<LocalTime> availableSlots;

    // A list of time slots that the doctor has blocked (or marked as busy).
    @ElementCollection
    @CollectionTable(name = "availability_busy_slots", joinColumns = @JoinColumn(name = "availability_id"))
    @Column(name = "time_slot")
    private List<LocalTime> busySlots;
}
