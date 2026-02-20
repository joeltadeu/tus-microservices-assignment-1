package com.jts.pmanagement.domains.doctor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jts.pmanagement.domains.doctor.dto.DoctorFilter;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.doctor.model.Speciality;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;

@DataJpaTest
@DisplayName("Doctor Repository Unit Tests")
class DoctorRepositoryImplTest {

  @Autowired private DoctorRepository doctorRepository;
  @Autowired private SpecialityRepository specialityRepository;

  private Speciality cardiology;
  private Speciality neurology;

  @BeforeEach
  void setUp() {
    doctorRepository.deleteAll();
    specialityRepository.deleteAll();

    cardiology =
        specialityRepository.findAll().stream()
            .filter(s -> s.getDescription().equals("Cardiology"))
            .findFirst()
            .orElseGet(
                () ->
                    specialityRepository.save(
                        Speciality.builder().description("Cardiology").build()));

    neurology =
        specialityRepository.findAll().stream()
            .filter(s -> s.getDescription().equals("Neurology"))
            .findFirst()
            .orElseGet(
                () ->
                    specialityRepository.save(
                        Speciality.builder().description("Neurology").build()));

    Doctor doctor1 =
        Doctor.builder()
            .firstName("John")
            .lastName("Doe")
            .title("Dr.")
            .email("john.doe@example.com")
            .phone("1111")
            .department("Heart")
            .speciality(cardiology)
            .createdAt(LocalDateTime.now().minusDays(2))
            .build();

    Doctor doctor2 =
        Doctor.builder()
            .firstName("Jane")
            .lastName("Smith")
            .title("Dr.")
            .email("jane.smith@example.com")
            .phone("2222")
            .department("Brain")
            .speciality(neurology)
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();

    Doctor doctor3 =
        Doctor.builder()
            .firstName("Johnny")
            .lastName("Appleseed")
            .title("Dr.")
            .email("johnny.apple@example.com")
            .phone("3333")
            .department("Heart")
            .speciality(cardiology)
            .createdAt(LocalDateTime.now())
            .build();

    doctorRepository.save(doctor1);
    doctorRepository.save(doctor2);
    doctorRepository.save(doctor3);
  }

  // =========================
  // BASIC CRUD TESTS
  // =========================

  @Test
  @DisplayName("save - should persist doctor and generate ID")
  void shouldSaveDoctor() {
    Doctor doctor =
        Doctor.builder()
            .firstName("Alice")
            .lastName("Brown")
            .title("Dr.")
            .email("alice.brown@example.com")
            .phone("4444")
            .department("General")
            .speciality(cardiology)
            .createdAt(LocalDateTime.now())
            .build();

    Doctor saved = doctorRepository.save(doctor);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getEmail()).isEqualTo("alice.brown@example.com");
    assertThat(saved.getSpeciality().getDescription()).isEqualTo("Cardiology");
  }

  @Test
  @DisplayName("findById - should return doctor when exists")
  void shouldFindById() {
    Doctor saved =
        doctorRepository.save(
            Doctor.builder()
                .firstName("Carlos")
                .lastName("Silva")
                .title("Dr.")
                .email("carlos.silva@example.com")
                .phone("5555")
                .department("General")
                .speciality(cardiology)
                .createdAt(LocalDateTime.now())
                .build());

    Optional<Doctor> found = doctorRepository.findById(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getFirstName()).isEqualTo("Carlos");
    assertThat(found.get().getSpeciality().getDescription()).isEqualTo("Cardiology");
  }

  @Test
  @DisplayName("delete - should remove doctor from database")
  void shouldDeleteDoctor() {
    Doctor saved =
        doctorRepository.save(
            Doctor.builder()
                .firstName("Mario")
                .lastName("Rossi")
                .title("Dr.")
                .email("mario.rossi@example.com")
                .phone("6666")
                .department("General")
                .speciality(neurology)
                .createdAt(LocalDateTime.now())
                .build());

    doctorRepository.delete(saved);

    Optional<Doctor> deleted = doctorRepository.findById(saved.getId());

    assertThat(deleted).isEmpty();
  }

  // =========================
  // FILTER TESTS
  // =========================

  @Test
  @DisplayName("findAllWithFilters - should filter by firstName (partial match)")
  void shouldFilterByFirstName() {
    DoctorFilter filter = new DoctorFilter();
    filter.setFirstName("John");

    Page<Doctor> result = doctorRepository.findAllWithFilters(filter);

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent())
        .extracting(Doctor::getFirstName)
        .containsExactlyInAnyOrder("John", "Johnny");
  }

  @Test
  @DisplayName("findAllWithFilters - should filter by email")
  void shouldFilterByEmail() {
    DoctorFilter filter = new DoctorFilter();
    filter.setEmail("jane.smith@example.com");

    Page<Doctor> result = doctorRepository.findAllWithFilters(filter);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().getFirst().getFirstName()).isEqualTo("Jane");
  }

  @Test
  @DisplayName("findAllWithFilters - should filter by speciality description")
  void shouldFilterBySpeciality() {
    DoctorFilter filter = new DoctorFilter();
    filter.setSpeciality("Cardio");

    Page<Doctor> result = doctorRepository.findAllWithFilters(filter);

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent())
        .extracting(d -> d.getSpeciality().getDescription())
        .containsOnly("Cardiology");
  }

  @Test
  @DisplayName("findAllWithFilters - should return paginated results")
  void shouldReturnPaginatedResults() {
    DoctorFilter filter = new DoctorFilter();
    filter.setPageNumber(0);
    filter.setPageSize(2);

    Page<Doctor> result = doctorRepository.findAllWithFilters(filter);

    assertThat(result.getTotalElements()).isEqualTo(3);
    assertThat(result.getContent()).hasSize(2);
  }

  @Test
  @DisplayName("findAllWithFilters - should sort by createdAt descending")
  void shouldSortByCreatedAtDescending() {
    DoctorFilter filter = new DoctorFilter();
    filter.setPageNumber(0);
    filter.setPageSize(10);

    Page<Doctor> result = doctorRepository.findAllWithFilters(filter);

    assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Johnny");
    assertThat(result.getContent().get(1).getFirstName()).isEqualTo("Jane");
    assertThat(result.getContent().get(2).getFirstName()).isEqualTo("John");
  }
}
