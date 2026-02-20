package com.jts.pmanagement.domains.doctor.controller.mapper;

import com.jts.pmanagement.domains.doctor.dto.DoctorRequest;
import com.jts.pmanagement.domains.doctor.dto.DoctorResponse;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.doctor.model.Speciality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

  public Doctor toDoctor(@Valid @NotNull DoctorRequest request) {
    return Doctor.builder()
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .title(request.getTitle())
        .speciality(Speciality.builder().id(request.getSpecialityId()).build())
        .email(request.getEmail())
        .phone(request.getPhone())
        .department(request.getDepartment())
        .createdAt(LocalDateTime.now())
        .build();
  }

  public DoctorResponse toDoctorResponse(@NotNull Doctor doctor) {
    return new DoctorResponse(
        doctor.getId(),
        doctor.getFirstName(),
        doctor.getLastName(),
        doctor.getTitle(),
        doctor.getSpeciality() != null ? doctor.getSpeciality().getDescription() : null,
        doctor.getEmail(),
        doctor.getPhone(),
        doctor.getDepartment());
  }
}
