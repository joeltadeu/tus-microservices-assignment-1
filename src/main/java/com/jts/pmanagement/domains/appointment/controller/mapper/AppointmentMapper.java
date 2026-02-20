package com.jts.pmanagement.domains.appointment.controller.mapper;

import com.jts.pmanagement.domains.appointment.dto.AppointmentRequest;
import com.jts.pmanagement.domains.appointment.dto.AppointmentResponse;
import com.jts.pmanagement.domains.appointment.dto.CancelAppointmentRequest;
import com.jts.pmanagement.domains.appointment.dto.DoctorAppointment;
import com.jts.pmanagement.domains.appointment.dto.PatientAppointment;
import com.jts.pmanagement.domains.appointment.model.Appointment;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.patient.model.Patient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

  public Appointment toAppointment(
      @NotNull Long id, @NotNull Long patientId, @Valid @NotNull CancelAppointmentRequest request) {
    return Appointment.builder()
        .id(id)
        .patient(Patient.builder().id(patientId).build())
        .cancellationReason(request.getReason())
        .build();
  }

  public Appointment toAppointment(
      @NotNull Long patientId, @Valid @NotNull AppointmentRequest request) {
    return Appointment.builder()
        .patient(Patient.builder().id(patientId).build())
        .doctor(Doctor.builder().id(request.getDoctorId()).build())
        .startTime(request.getStartTime())
        .type(request.getType())
        .title(request.getTitle())
        .description(request.getDescription())
        .build();
  }

  public AppointmentResponse toAppointmentResponse(Appointment appointment) {
    return AppointmentResponse.builder()
        .id(appointment.getId())
        .patient(toPatientAppointment(appointment.getPatient()))
        .doctor(toDoctorAppointment(appointment.getDoctor()))
        .startTime(appointment.getStartTime())
        .endTime(appointment.getEndTime())
        .duration(appointment.getDuration())
        .description(appointment.getDescription())
        .title(appointment.getTitle())
        .type(appointment.getType())
        .status(appointment.getStatus())
        .cancellationReason(appointment.getCancellationReason())
        .cancellationTime(appointment.getCancellationTime())
        .build();
  }

  private PatientAppointment toPatientAppointment(Patient patient) {
    if (patient == null) {
      return null;
    }

    return new PatientAppointment(
        patient.getId(), patient.getFirstName(), patient.getLastName(), patient.getEmail());
  }

  private DoctorAppointment toDoctorAppointment(Doctor doctor) {
    if (doctor == null) {
      return null;
    }

    return new DoctorAppointment(
        doctor.getId(),
        doctor.getFirstName(),
        doctor.getLastName(),
        doctor.getTitle(),
        doctor.getSpeciality() != null ? doctor.getSpeciality().getDescription() : null);
  }
}
