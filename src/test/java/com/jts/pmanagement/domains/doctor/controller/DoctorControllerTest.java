package com.jts.pmanagement.domains.doctor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jts.pmanagement.common.exception.NotFoundException;
import com.jts.pmanagement.domains.doctor.controller.mapper.DoctorMapper;
import com.jts.pmanagement.domains.doctor.dto.DoctorFilter;
import com.jts.pmanagement.domains.doctor.dto.DoctorRequest;
import com.jts.pmanagement.domains.doctor.dto.DoctorResponse;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.doctor.model.Speciality;
import com.jts.pmanagement.domains.doctor.service.DoctorService;
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

@WebMvcTest(DoctorController.class)
@DisplayName("Doctor Controller Unit Test")
class DoctorControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DoctorService service;

  @MockitoBean private DoctorMapper doctorMapper;

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private Doctor buildDoctor(Long id) {
    return Doctor.builder()
        .id(id)
        .firstName("John")
        .lastName("Foreman")
        .title("Dr.")
        .speciality(Speciality.builder().id(1L).description("Cardiology").build())
        .email("john@email.com")
        .phone("123456")
        .department("Primary Care")
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("POST /v1/doctors - should create doctor successfully")
  void shouldInsertDoctorSuccessfully() throws Exception {

    DoctorRequest request =
        new DoctorRequest("John", "Foreman", "Dr.", 1L, "john@email.com", "123456", "Primary Care");

    Doctor doctor = buildDoctor(1L);

    DoctorResponse response =
        new DoctorResponse(
            1L, "John", "Foreman", "Dr.", "Cardiology", "john@email.com", "123456", "Primary Care");

    when(doctorMapper.toDoctor(any(DoctorRequest.class))).thenReturn(doctor);
    doNothing().when(service).insert(any(Doctor.class));
    when(doctorMapper.toDoctorResponse(any(Doctor.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/v1/doctors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.firstName").value("John"));

    verify(service).insert(any(Doctor.class));
  }

  @Test
  @DisplayName("POST /v1/doctors - should return 400 when request is invalid")
  void shouldReturnBadRequestWhenValidationFails() throws Exception {

    DoctorRequest invalidRequest = new DoctorRequest("", "", "", null, "invalid-email", null, null);

    mockMvc
        .perform(
            post("/v1/doctors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT /v1/doctors/{id} - should update doctor successfully")
  void shouldUpdateDoctorSuccessfully() throws Exception {

    Long id = 1L;

    DoctorRequest request =
        new DoctorRequest("John", "Foreman", "Dr.", 1L, "john@email.com", "123456", "Primary Care");

    Doctor doctor = buildDoctor(id);

    when(doctorMapper.toDoctor(any(DoctorRequest.class))).thenReturn(doctor);
    doNothing().when(service).update(eq(id), any(Doctor.class));

    mockMvc
        .perform(
            put("/v1/doctors/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(service).update(eq(id), any(Doctor.class));
  }

  @Test
  @DisplayName("DELETE /v1/doctors/{id} - should delete doctor successfully")
  void shouldDeleteDoctorSuccessfully() throws Exception {

    Long id = 1L;

    doNothing().when(service).delete(id);

    mockMvc.perform(delete("/v1/doctors/{id}", id)).andExpect(status().isNoContent());

    verify(service).delete(id);
  }

  @Test
  @DisplayName("GET /v1/doctors/{id} - should return doctor when found")
  void shouldReturnDoctorById() throws Exception {

    Long id = 1L;

    Doctor doctor = buildDoctor(id);

    DoctorResponse response =
        new DoctorResponse(
            id, "John", "Foreman", "Dr.", "Cardiology", "john@email.com", "123456", "Primary Care");

    when(service.findById(id)).thenReturn(doctor);
    when(doctorMapper.toDoctorResponse(doctor)).thenReturn(response);

    mockMvc
        .perform(get("/v1/doctors/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.firstName").value("John"));

    verify(service).findById(id);
  }

  @Test
  @DisplayName("GET /v1/doctors/{id} - should return 404 when not found")
  void shouldReturnNotFoundWhenDoctorDoesNotExist() throws Exception {

    Long id = 99L;

    when(service.findById(id)).thenThrow(new NotFoundException("Doctor not found"));

    mockMvc.perform(get("/v1/doctors/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /v1/doctors - should return paginated list of doctors")
  void shouldReturnDoctorList() throws Exception {

    Doctor doctor = buildDoctor(1L);

    DoctorResponse response =
        new DoctorResponse(
            1L, "John", "Foreman", "Dr.", "Cardiology", "john@email.com", "123456", "Primary Care");

    Page<Doctor> page = new PageImpl<>(List.of(doctor));

    when(service.findAll(any(DoctorFilter.class))).thenReturn(page);
    when(doctorMapper.toDoctorResponse(doctor)).thenReturn(response);

    mockMvc
        .perform(get("/v1/doctors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].firstName").value("John"));

    verify(service).findAll(any(DoctorFilter.class));
  }
}
