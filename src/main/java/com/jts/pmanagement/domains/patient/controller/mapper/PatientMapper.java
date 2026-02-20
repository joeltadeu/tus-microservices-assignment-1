package com.jts.pmanagement.domains.patient.controller.mapper;

import com.jts.pmanagement.domains.patient.dto.PatientRequest;
import com.jts.pmanagement.domains.patient.dto.PatientResponse;
import com.jts.pmanagement.domains.patient.model.Patient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PatientMapper {
  public Patient toPatient(@Valid @NotNull PatientRequest request) {
    return Patient.builder()
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .email(request.getEmail())
        .address(request.getAddress())
        .dateOfBirth(request.getDateOfBirth())
        .createdAt(LocalDateTime.now())
        .build();
  }

  public PatientResponse toPatientResponse(@NotNull Patient patient) {
    return new PatientResponse(
        patient.getId(),
        patient.getFirstName(),
        patient.getLastName(),
        patient.getEmail(),
        patient.getAddress(),
        patient.getDateOfBirth());
  }
}
