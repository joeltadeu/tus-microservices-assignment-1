package com.jts.pmanagement.domains.doctor.controller.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jts.pmanagement.domains.doctor.dto.DoctorRequest;
import com.jts.pmanagement.domains.doctor.dto.DoctorResponse;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.doctor.model.Speciality;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Doctor Mapper Unit Test")
class DoctorMapperTest {

  private final DoctorMapper mapper = new DoctorMapper();

  @Test
  @DisplayName("Should map DoctorRequest to Doctor entity correctly")
  void shouldMapRequestToEntity() {

    DoctorRequest request =
        new DoctorRequest("John", "Foreman", "Dr.", 5L, "john@email.com", "+1-555", "Primary Care");

    Doctor doctor = mapper.toDoctor(request);

    assertNotNull(doctor);
    assertEquals("John", doctor.getFirstName());
    assertEquals("Foreman", doctor.getLastName());
    assertEquals("Dr.", doctor.getTitle());
    assertEquals("john@email.com", doctor.getEmail());
    assertEquals("+1-555", doctor.getPhone());
    assertEquals("Primary Care", doctor.getDepartment());

    assertNotNull(doctor.getSpeciality());
    assertEquals(5L, doctor.getSpeciality().getId());

    assertNotNull(doctor.getCreatedAt());
  }

  @Test
  @DisplayName("Should map Doctor entity to DoctorResponse including speciality description")
  void shouldMapEntityToResponseWithSpeciality() {

    Speciality speciality = Speciality.builder().id(1L).description("Cardiology").build();

    Doctor doctor =
        Doctor.builder()
            .id(10L)
            .firstName("John")
            .lastName("Foreman")
            .title("Dr.")
            .speciality(speciality)
            .email("john@email.com")
            .phone("123")
            .department("Cardio")
            .createdAt(LocalDateTime.now())
            .build();

    DoctorResponse response = mapper.toDoctorResponse(doctor);

    assertNotNull(response);
    assertEquals(10L, response.getId());
    assertEquals("Cardiology", response.getSpeciality());
  }

  @Test
  @DisplayName("Should map Doctor entity to DoctorResponse with null speciality safely")
  void shouldHandleNullSpeciality() {

    Doctor doctor =
        Doctor.builder()
            .id(10L)
            .firstName("John")
            .lastName("Foreman")
            .title("Dr.")
            .email("john@email.com")
            .phone("123")
            .department("General")
            .createdAt(LocalDateTime.now())
            .build();

    DoctorResponse response = mapper.toDoctorResponse(doctor);

    assertNull(response.getSpeciality());
  }
}
