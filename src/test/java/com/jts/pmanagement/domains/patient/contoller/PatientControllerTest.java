package com.jts.pmanagement.domains.patient.contoller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jts.pmanagement.common.exception.NotFoundException;
import com.jts.pmanagement.domains.patient.controller.PatientController;
import com.jts.pmanagement.domains.patient.controller.mapper.PatientMapper;
import com.jts.pmanagement.domains.patient.dto.PatientFilter;
import com.jts.pmanagement.domains.patient.dto.PatientRequest;
import com.jts.pmanagement.domains.patient.dto.PatientResponse;
import com.jts.pmanagement.domains.patient.model.Patient;
import com.jts.pmanagement.domains.patient.service.PatientService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PatientController.class)
@DisplayName("Patient Controller Unit Tests")
class PatientControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PatientService service;

  @MockitoBean private PatientMapper patientMapper;

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private Patient buildPatient(Long id) {
    return Patient.builder()
        .id(id)
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@gmail.com")
        .address("Street 1")
        .dateOfBirth(LocalDate.of(1990, 1, 1))
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("POST /v1/patients - should create patient successfully")
  void shouldInsertPatientSuccessfully() throws Exception {

    PatientRequest request =
        new PatientRequest(
            "John", "Doe", "john.doe@gmail.com", "Street 1", LocalDate.of(1990, 1, 1));

    Patient patient = buildPatient(1L);

    PatientResponse response =
        new PatientResponse(
            1L, "John", "Doe", "john.doe@gmail.com", "Street 1", LocalDate.of(1990, 1, 1));

    when(patientMapper.toPatient(any(PatientRequest.class))).thenReturn(patient);
    doNothing().when(service).insert(any(Patient.class));
    when(patientMapper.toPatientResponse(any(Patient.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.firstName").value("John"))
        .andExpect(jsonPath("$.lastName").value("Doe"))
        .andExpect(jsonPath("$.email").value("john.doe@gmail.com"));
  }

  @Test
  @DisplayName("POST /v1/patients - should return 400 when request is invalid")
  void insert_shouldReturnBadRequest_whenValidationFails() throws Exception {

    PatientRequest invalidRequest =
        new PatientRequest(
            "", // invalid firstName
            "Doe",
            "invalid-email", // invalid email
            "Street 1",
            null // invalid dateOfBirth
            );

    mockMvc
        .perform(
            post("/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT /v1/patients/{id} - should update patient successfully")
  void shouldUpdatePatientSuccessfully() throws Exception {
    Long id = 1L;

    PatientRequest request =
        new PatientRequest(
            "John", "Doe", "john.doe@gmail.com", "Street 1", LocalDate.of(1990, 1, 1));

    Patient patient = buildPatient(id);

    when(patientMapper.toPatient(any(PatientRequest.class))).thenReturn(patient);
    doNothing().when(service).update(eq(id), any(Patient.class));

    mockMvc
        .perform(
            put("/v1/patients/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(service).update(eq(id), any(Patient.class));
  }

  @Test
  @DisplayName("DELETE /v1/patients/{id} - should delete patient successfully")
  void shouldDeletePatientSuccessfully() throws Exception {
    Long patientId = 1L;

    doNothing().when(service).delete(patientId);

    mockMvc.perform(delete("/v1/patients/{id}", patientId)).andExpect(status().isNoContent());

    verify(service).delete(patientId);
  }

  @Test
  @DisplayName("GET /v1/patients/{id} - should return patient when found")
  void findById_shouldReturnPatient() throws Exception {

    Long patientId = 1L;

    Patient patient = buildPatient(patientId);

    PatientResponse response =
        new PatientResponse(
            1L, "John", "Doe", "john.doe@gmail.com", "Street 1", LocalDate.of(1990, 1, 1));

    when(service.findById(patientId)).thenReturn(patient);
    when(patientMapper.toPatientResponse(patient)).thenReturn(response);

    mockMvc
        .perform(get("/v1/patients/{id}", patientId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(patientId))
        .andExpect(jsonPath("$.firstName").value("John"))
        .andExpect(jsonPath("$.lastName").value("Doe"))
        .andExpect(jsonPath("$.email").value("john.doe@gmail.com"));
  }

  @Test
  @DisplayName("GET /v1/patients/{id} - should return 404 when not found")
  void findById_shouldReturnNotFound() throws Exception {

    Long patientId = 99L;

    when(service.findById(patientId)).thenThrow(new NotFoundException("Patient not found"));

    mockMvc.perform(get("/v1/patients/{id}", patientId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /v1/patients - should return paginated list of patients")
  void shouldReturnPatientList() throws Exception {
    Patient patient = buildPatient(1L);

    PatientResponse response =
        new PatientResponse(
            1L, "John", "Doe", "john.doe@gmail.com", "Street 1", LocalDate.of(1990, 1, 1));

    Page<Patient> page = new PageImpl<>(List.of(patient));
    when(service.findAll(any(PatientFilter.class))).thenReturn(page);
    when(patientMapper.toPatientResponse(patient)).thenReturn(response);

    mockMvc
        .perform(get("/v1/patients"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].firstName").value("John"));

    verify(service).findAll(any(PatientFilter.class));
  }
}
