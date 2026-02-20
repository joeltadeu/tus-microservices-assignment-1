package com.jts.pmanagement.domains.patient.model;

import com.jts.pmanagement.domains.appointment.model.Appointment;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString(exclude = "appointments")
@EqualsAndHashCode(exclude = "appointments")
public class Patient {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String firstName;

  @NotNull private String lastName;

  @NotNull
  @Email
  @Column(unique = true)
  private String email;

  @NotNull private String address;

  @NotNull private LocalDate dateOfBirth;

  @NotNull private LocalDateTime createdAt;

  @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Appointment> appointments;
}
