package com.jts.pmanagement.domains.patient.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jts.pmanagement.domains.patient.dto.PatientFilter;
import com.jts.pmanagement.domains.patient.model.Patient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;

@DataJpaTest
@DisplayName("Patient Repository Unit Tests")
class PatientRepositoryImplTest {

  @Autowired private PatientRepository repository;

  @BeforeEach
  void setUp() {
    repository.deleteAll();

    Patient patient1 =
        Patient.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .address("123 Main St")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .createdAt(LocalDateTime.now().minusDays(2))
            .build();

    Patient patient2 =
        Patient.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .address("456 Elm St")
            .dateOfBirth(LocalDate.of(1992, 5, 10))
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();

    Patient patient3 =
        Patient.builder()
            .firstName("Johnny")
            .lastName("Appleseed")
            .email("johnny.apple@example.com")
            .address("789 Oak St")
            .dateOfBirth(LocalDate.of(1985, 7, 20))
            .createdAt(LocalDateTime.now())
            .build();

    repository.save(patient1);
    repository.save(patient2);
    repository.save(patient3);
  }

  // =========================
  // BASIC CRUD TESTS
  // =========================

  @Test
  @DisplayName("save - should persist patient and generate ID")
  void shouldSavePatient() {
    Patient patient =
        Patient.builder()
            .firstName("Alice")
            .lastName("Brown")
            .email("alice.brown@example.com")
            .address("321 Pine St")
            .dateOfBirth(LocalDate.of(1995, 3, 15))
            .createdAt(LocalDateTime.now())
            .build();

    Patient saved = repository.save(patient);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getFirstName()).isEqualTo("Alice");
    assertThat(saved.getEmail()).isEqualTo("alice.brown@example.com");
  }

  @Test
  @DisplayName("findById - should return patient when exists")
  void shouldFindById() {
    Patient saved =
        repository.save(
            Patient.builder()
                .firstName("Carlos")
                .lastName("Silva")
                .email("carlos.silva@example.com")
                .address("New Address")
                .dateOfBirth(LocalDate.of(1988, 8, 8))
                .createdAt(LocalDateTime.now())
                .build());

    Optional<Patient> found = repository.findById(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("carlos.silva@example.com");
  }

  @Test
  @DisplayName("delete - should remove patient from database")
  void shouldDeletePatient() {
    Patient saved =
        repository.save(
            Patient.builder()
                .firstName("Mario")
                .lastName("Rossi")
                .email("mario.rossi@example.com")
                .address("Delete St")
                .dateOfBirth(LocalDate.of(1980, 10, 10))
                .createdAt(LocalDateTime.now())
                .build());

    repository.delete(saved);

    Optional<Patient> deleted = repository.findById(saved.getId());

    assertThat(deleted).isEmpty();
  }

  // =========================
  // FILTER TESTS
  // =========================

  @Test
  @DisplayName("findAllWithFilters - should filter by firstName (partial match)")
  void shouldFilterByFirstName() {
    PatientFilter filter = new PatientFilter();
    filter.setFirstName("John");

    Page<Patient> result = repository.findAllWithFilters(filter);

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent())
        .extracting(Patient::getFirstName)
        .containsExactlyInAnyOrder("John", "Johnny");
  }

  @Test
  @DisplayName("findAllWithFilters - should filter by exact email")
  void shouldFilterByEmail() {
    PatientFilter filter = new PatientFilter();
    filter.setEmail("jane.smith@example.com");

    Page<Patient> result = repository.findAllWithFilters(filter);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().getFirst().getFirstName()).isEqualTo("Jane");
  }

  @Test
  @DisplayName("findAllWithFilters - should return paginated results")
  void shouldReturnAllPatientsWithPagination() {
    PatientFilter filter = new PatientFilter();
    filter.setPageNumber(0);
    filter.setPageSize(2);

    Page<Patient> result = repository.findAllWithFilters(filter);

    assertThat(result.getTotalElements()).isEqualTo(3);
    assertThat(result.getContent()).hasSize(2);
  }

  @Test
  @DisplayName("findAllWithFilters - should sort by createdAt descending")
  void shouldSortByCreatedAtDescending() {
    PatientFilter filter = new PatientFilter();
    filter.setPageNumber(0);
    filter.setPageSize(10);

    Page<Patient> result = repository.findAllWithFilters(filter);

    assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Johnny");
    assertThat(result.getContent().get(1).getFirstName()).isEqualTo("Jane");
    assertThat(result.getContent().get(2).getFirstName()).isEqualTo("John");
  }
}
