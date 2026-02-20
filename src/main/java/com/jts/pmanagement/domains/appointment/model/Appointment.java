package com.jts.pmanagement.domains.appointment.model;

import com.jts.pmanagement.domains.appointment.dto.AppointmentStatus;
import com.jts.pmanagement.domains.appointment.dto.AppointmentType;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.patient.model.Patient;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Appointment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "patient_id")
  private Patient patient;

  @ManyToOne
  @JoinColumn(name = "doctor_id")
  private Doctor doctor;

  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer duration;

  @Enumerated(EnumType.STRING)
  private AppointmentType type;

  private String title;
  private String description;

  @Enumerated(EnumType.STRING)
  private AppointmentStatus status;

  private Boolean followUpRequired;
  private LocalDateTime cancellationTime;
  private String cancellationReason;
  private LocalDateTime createdAt;
  private LocalDateTime lastUpdated;
}
