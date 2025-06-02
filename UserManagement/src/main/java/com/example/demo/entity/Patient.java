package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "patients")
public class Patient {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")  // Explicitly mapped column name
    private Long patientId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String disease;
    private String place;
}
