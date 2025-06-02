package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_id")  // explicitly map to doctor_id column
    private Long doctorId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String specialization;
    private String qualification;
    private String roomNumber;
}
