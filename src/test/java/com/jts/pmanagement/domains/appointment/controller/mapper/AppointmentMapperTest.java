package com.jts.pmanagement.domains.appointment.controller.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.jts.pmanagement.domains.appointment.dto.AppointmentRequest;
import com.jts.pmanagement.domains.appointment.dto.AppointmentResponse;
import com.jts.pmanagement.domains.appointment.dto.AppointmentStatus;
import com.jts.pmanagement.domains.appointment.dto.AppointmentType;
import com.jts.pmanagement.domains.appointment.dto.CancelAppointmentRequest;
import com.jts.pmanagement.domains.appointment.model.Appointment;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.doctor.model.Speciality;
import com.jts.pmanagement.domains.patient.model.Patient;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Appointment Mapper Unit Test")
class AppointmentMapperTest {

  private final AppointmentMapper mapper = new AppointmentMapper();

  @Test
  @DisplayName("Should map AppointmentRequest to Appointment entity correctly")
  void shouldMapRequestToEntity() {

    LocalDateTime startTime = LocalDateTime.now();

    AppointmentRequest request =
        new AppointmentRequest(2L, startTime, AppointmentType.CONSULTATION, "Title", "Desc");

    Appointment appointment = mapper.toAppointment(1L, request);

    assertNotNull(appointment);
    assertEquals(1L, appointment.getPatient().getId());
    assertEquals(2L, appointment.getDoctor().getId());
    assertEquals(startTime, appointment.getStartTime());
    assertEquals("Title", appointment.getTitle());
    assertEquals("Desc", appointment.getDescription());
    assertEquals(AppointmentType.CONSULTATION, appointment.getType());
  }

  @Test
  @DisplayName("Should map CancelAppointmentRequest to Appointment entity correctly")
  void shouldMapCancelRequest() {

    CancelAppointmentRequest request = new CancelAppointmentRequest();
    request.setReason("Emergency");

    Appointment appointment = mapper.toAppointment(10L, 1L, request);

    assertEquals(10L, appointment.getId());
    assertEquals(1L, appointment.getPatient().getId());
    assertEquals("Emergency", appointment.getCancellationReason());
  }

  @Test
  @DisplayName(
      "Should map Appointment entity to AppointmentResponse including nested doctor and patient")
  void shouldMapEntityToResponse() {

    Speciality speciality = Speciality.builder().id(1L).description("Cardiology").build();

    Appointment appointment =
        Appointment.builder()
            .id(100L)
            .patient(
                Patient.builder()
                    .id(1L)
                    .firstName("Alice")
                    .lastName("Smith")
                    .email("alice@email.com")
                    .build())
            .doctor(
                Doctor.builder()
                    .id(2L)
                    .firstName("John")
                    .lastName("Foreman")
                    .title("Dr.")
                    .speciality(speciality)
                    .build())
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now().plusHours(1))
            .duration(60)
            .type(AppointmentType.CONSULTATION)
            .status(AppointmentStatus.SCHEDULED)
            .cancellationReason("Emergency")
            .build();

    AppointmentResponse response = mapper.toAppointmentResponse(appointment);

    assertNotNull(response);
    assertEquals(100L, response.getId());
    assertEquals("Alice", response.getPatient().getFirstName());
    assertEquals("John", response.getDoctor().getFirstName());
    assertEquals("Cardiology", response.getDoctor().getSpeciality());
    assertEquals(AppointmentStatus.SCHEDULED, response.getStatus());
  }

  @Test
  @DisplayName("Should handle null doctor and patient safely in response mapping")
  void shouldHandleNullNestedObjects() {

    Appointment appointment = Appointment.builder().id(1L).build();

    AppointmentResponse response = mapper.toAppointmentResponse(appointment);

    assertNull(response.getDoctor());
    assertNull(response.getPatient());
  }
}
