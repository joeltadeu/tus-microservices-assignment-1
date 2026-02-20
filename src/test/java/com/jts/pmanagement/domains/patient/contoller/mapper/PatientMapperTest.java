package com.jts.pmanagement.domains.patient.controller.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jts.pmanagement.domains.patient.dto.PatientRequest;
import com.jts.pmanagement.domains.patient.dto.PatientResponse;
import com.jts.pmanagement.domains.patient.model.Patient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Patient Mapper Unit Test")
class PatientMapperTest {

  private final PatientMapper mapper = new PatientMapper();

  @Test
  @DisplayName("Should map PatientRequest to Patient entity correctly")
  void shouldMapRequestToEntity() {

    LocalDate birthDate = LocalDate.of(1990, 1, 1);

    PatientRequest request =
        new PatientRequest("Alice", "Smith", "alice@email.com", "Street 1", birthDate);

    Patient patient = mapper.toPatient(request);

    assertNotNull(patient);
    assertEquals("Alice", patient.getFirstName());
    assertEquals("Smith", patient.getLastName());
    assertEquals("alice@email.com", patient.getEmail());
    assertEquals("Street 1", patient.getAddress());
    assertEquals(birthDate, patient.getDateOfBirth());
    assertNotNull(patient.getCreatedAt());
  }

  @Test
  @DisplayName("Should map Patient entity to PatientResponse correctly")
  void shouldMapEntityToResponse() {

    LocalDate birthDate = LocalDate.of(1990, 1, 1);

    Patient patient =
        Patient.builder()
            .id(1L)
            .firstName("Alice")
            .lastName("Smith")
            .email("alice@email.com")
            .address("Street 1")
            .dateOfBirth(birthDate)
            .createdAt(LocalDateTime.now())
            .build();

    PatientResponse response = mapper.toPatientResponse(patient);

    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("Alice", response.getFirstName());
    assertEquals("Smith", response.getLastName());
    assertEquals("Street 1", response.getAddress());
    assertEquals(birthDate, response.getDateOfBirth());
  }
}
