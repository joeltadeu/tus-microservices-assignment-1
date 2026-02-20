package com.jts.pmanagement.domains.appointment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jts.pmanagement.common.exception.NotFoundException;
import com.jts.pmanagement.domains.appointment.controller.mapper.AppointmentMapper;
import com.jts.pmanagement.domains.appointment.dto.AppointmentFilter;
import com.jts.pmanagement.domains.appointment.dto.AppointmentRequest;
import com.jts.pmanagement.domains.appointment.dto.AppointmentResponse;
import com.jts.pmanagement.domains.appointment.dto.AppointmentStatus;
import com.jts.pmanagement.domains.appointment.dto.AppointmentType;
import com.jts.pmanagement.domains.appointment.dto.CancelAppointmentRequest;
import com.jts.pmanagement.domains.appointment.model.Appointment;
import com.jts.pmanagement.domains.appointment.service.AppointmentService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppointmentController.class)
@DisplayName("Appointment Controller Unit Test")
class AppointmentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AppointmentService service;

  @MockitoBean private AppointmentMapper mapper;

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private final Long patientId = 1L;
  private final Long appointmentId = 10L;

  private Appointment buildAppointment() {
    return Appointment.builder()
        .id(appointmentId)
        .startTime(LocalDateTime.now())
        .status(AppointmentStatus.SCHEDULED)
        .build();
  }

  private AppointmentResponse buildResponse() {
    return AppointmentResponse.builder()
        .id(appointmentId)
        .status(AppointmentStatus.SCHEDULED)
        .build();
  }

  // ========================= POST CREATE =========================

  @Test
  @DisplayName(
      "POST /v1/patients/{patientId}/appointments - should register appointment successfully")
  void shouldCreateAppointment() throws Exception {

    AppointmentRequest request =
        new AppointmentRequest(
            2L, LocalDateTime.now(), AppointmentType.CONSULTATION, "Title", "Desc");

    Appointment appointment = buildAppointment();
    AppointmentResponse response = buildResponse();

    when(mapper.toAppointment(eq(patientId), any(AppointmentRequest.class)))
        .thenReturn(appointment);
    when(service.insert(any(Appointment.class))).thenReturn(appointment);
    when(mapper.toAppointmentResponse(appointment)).thenReturn(response);

    mockMvc
        .perform(
            post("/v1/patients/{patientId}/appointments", patientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName(
      "POST /v1/patients/{patientId}/appointments - should return 400 when request is invalid")
  void shouldReturnBadRequestWhenInvalid() throws Exception {

    AppointmentRequest invalidRequest = new AppointmentRequest(null, null, null, null, null);

    mockMvc
        .perform(
            post("/v1/patients/{patientId}/appointments", patientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  // ========================= PUT UPDATE =========================

  @Test
  @DisplayName(
      "PUT /v1/patients/{patientId}/appointments/{id} - should update appointment successfully")
  void shouldUpdateAppointment() throws Exception {

    AppointmentRequest request =
        new AppointmentRequest(
            2L, LocalDateTime.now(), AppointmentType.CONSULTATION, "Updated", "Updated");

    Appointment appointment = buildAppointment();
    AppointmentResponse response = buildResponse();

    when(service.update(eq(appointmentId), eq(patientId), any(AppointmentRequest.class)))
        .thenReturn(appointment);
    when(mapper.toAppointmentResponse(appointment)).thenReturn(response);

    mockMvc
        .perform(
            put("/v1/patients/{patientId}/appointments/{id}", patientId, appointmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  // ========================= CANCEL =========================

  @Test
  @DisplayName(
      "POST /v1/patients/{patientId}/appointments/{id}/cancel - should cancel appointment successfully")
  void shouldCancelAppointment() throws Exception {

    CancelAppointmentRequest request = new CancelAppointmentRequest();
    request.setReason("Emergency");

    Appointment appointment = buildAppointment();
    AppointmentResponse response = buildResponse();

    when(service.cancel(eq(appointmentId), eq(patientId), any(CancelAppointmentRequest.class)))
        .thenReturn(appointment);
    when(mapper.toAppointmentResponse(appointment)).thenReturn(response);

    mockMvc
        .perform(
            post("/v1/patients/{patientId}/appointments/{id}/cancel", patientId, appointmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  // ========================= GET BY ID =========================

  @Test
  @DisplayName(
      "GET /v1/patients/{patientId}/appointments/{id} - should return appointment when found")
  void shouldFindById() throws Exception {

    Appointment appointment = buildAppointment();
    AppointmentResponse response = buildResponse();

    when(service.findByIdEnriched(patientId, appointmentId)).thenReturn(appointment);
    when(mapper.toAppointmentResponse(appointment)).thenReturn(response);

    mockMvc
        .perform(get("/v1/patients/{patientId}/appointments/{id}", patientId, appointmentId))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /v1/patients/{patientId}/appointments/{id} - should return 404 when not found")
  void shouldReturnNotFound() throws Exception {

    when(service.findByIdEnriched(patientId, appointmentId))
        .thenThrow(new NotFoundException("Not found"));

    mockMvc
        .perform(get("/v1/patients/{patientId}/appointments/{id}", patientId, appointmentId))
        .andExpect(status().isNotFound());
  }

  // ========================= DELETE =========================

  @Test
  @DisplayName(
      "DELETE /v1/patients/{patientId}/appointments/{id} - should delete appointment successfully")
  void shouldDeleteAppointment() throws Exception {

    doNothing().when(service).delete(patientId, appointmentId);

    mockMvc
        .perform(delete("/v1/patients/{patientId}/appointments/{id}", patientId, appointmentId))
        .andExpect(status().isNoContent());
  }

  // ========================= LIST =========================

  @Test
  @DisplayName("GET /v1/patients/{patientId}/appointments - should return appointments list")
  void shouldListAppointments() throws Exception {

    Appointment appointment = buildAppointment();
    AppointmentResponse response = buildResponse();

    when(service.findAllByPatientId(eq(patientId), any(AppointmentFilter.class)))
        .thenReturn(new PageImpl<>(List.of(appointment)));

    when(mapper.toAppointmentResponse(appointment)).thenReturn(response);

    mockMvc
        .perform(get("/v1/patients/{patientId}/appointments", patientId))
        .andExpect(status().isOk());
  }
}
