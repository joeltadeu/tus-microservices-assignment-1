package com.jts.pmanagement.domains.appointment.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.jts.pmanagement.domains.appointment.dto.AppointmentFilter;
import com.jts.pmanagement.domains.appointment.dto.AppointmentStatus;
import com.jts.pmanagement.domains.appointment.dto.AppointmentType;
import com.jts.pmanagement.domains.appointment.model.Appointment;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.doctor.model.Speciality;
import com.jts.pmanagement.domains.doctor.repository.DoctorRepository;
import com.jts.pmanagement.domains.doctor.repository.SpecialityRepository;
import com.jts.pmanagement.domains.patient.model.Patient;
import com.jts.pmanagement.domains.patient.repository.PatientRepository;
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
@DisplayName("Appointment Repository Unit Tests")
class AppointmentRepositoryImplTest {

  @Autowired private AppointmentRepository appointmentRepository;

  @Autowired private DoctorRepository doctorRepository;

  @Autowired private PatientRepository patientRepository;

  @Autowired private SpecialityRepository specialityRepository;

  private Patient patient1;
  private Patient patient2;
  private Doctor doctor1;
  private Doctor doctor2;
  private Speciality cardiology;

  @BeforeEach
  void setUp() {
    doctorRepository.deleteAll();
    patientRepository.deleteAll();
    appointmentRepository.deleteAll();
    specialityRepository.deleteAll();

    cardiology =
        specialityRepository.findAll().stream()
            .filter(s -> s.getDescription().equals("Cardiology"))
            .findFirst()
            .orElseGet(
                () ->
                    specialityRepository.save(
                        Speciality.builder().description("Cardiology").build()));

    patient1 =
        patientRepository.findAll().stream()
            .filter(s -> s.getFirstName().equals("John"))
            .findFirst()
            .orElseGet(
                () ->
                    patientRepository.save(
                        Patient.builder()
                            .firstName("John")
                            .lastName("Doe")
                            .email("john.doe@gmail.com")
                            .address("Street 1")
                            .dateOfBirth(LocalDate.of(1990, 1, 1))
                            .createdAt(LocalDateTime.now())
                            .build()));

    patient2 =
        patientRepository.findAll().stream()
            .filter(s -> s.getFirstName().equals("Mark"))
            .findFirst()
            .orElseGet(
                () ->
                    patientRepository.save(
                        Patient.builder()
                            .firstName("Mark")
                            .lastName("Zorich")
                            .email("mark.zorich@gmail.com")
                            .address("Street 2")
                            .dateOfBirth(LocalDate.of(1983, 4, 20))
                            .createdAt(LocalDateTime.now())
                            .build()));

    doctor1 =
        doctorRepository.findAll().stream()
            .filter(s -> s.getFirstName().equals("John"))
            .findFirst()
            .orElseGet(
                () ->
                    doctorRepository.save(
                        Doctor.builder()
                            .firstName("John")
                            .lastName("Foreman")
                            .title("Dr.")
                            .speciality(cardiology)
                            .email("john.foreman@email.com")
                            .phone("123456")
                            .department("Primary Care")
                            .createdAt(LocalDateTime.now())
                            .build()));

    doctor2 =
        doctorRepository.findAll().stream()
            .filter(s -> s.getFirstName().equals("Carmen"))
            .findFirst()
            .orElseGet(
                () ->
                    doctorRepository.save(
                        Doctor.builder()
                            .firstName("Carmen")
                            .lastName("Louis")
                            .title("Dr.")
                            .speciality(cardiology)
                            .email("carmen.louis@email.com")
                            .phone("123456")
                            .department("Primary Care")
                            .createdAt(LocalDateTime.now())
                            .build()));
  }

  private Appointment createAppointment(
      Patient patient, Doctor doctor, LocalDateTime start, AppointmentStatus status) {

    Appointment appointment =
        Appointment.builder()
            .patient(patient)
            .doctor(doctor)
            .startTime(start)
            .endTime(start.plusHours(1))
            .duration(60)
            .type(AppointmentType.CONSULTATION)
            .title("Consultation")
            .description("General checkup")
            .status(status)
            .followUpRequired(false)
            .createdAt(LocalDateTime.now())
            .lastUpdated(LocalDateTime.now())
            .build();

    return appointmentRepository.save(appointment);
  }

  // -----------------------------
  // CRUD TESTS
  // -----------------------------

  @Test
  @DisplayName("Should save appointment successfully")
  void save_shouldPersistAppointment() {
    Appointment saved =
        createAppointment(patient1, doctor1, LocalDateTime.now(), AppointmentStatus.SCHEDULED);

    assertNotNull(saved.getId());
    assertEquals(patient1.getId(), saved.getPatient().getId());
    assertEquals(doctor1.getId(), saved.getDoctor().getId());
    assertEquals(AppointmentStatus.SCHEDULED, saved.getStatus());
  }

  @Test
  @DisplayName("Should find appointment by id")
  void findById_shouldReturnAppointment() {

    Appointment saved =
        createAppointment(patient1, doctor1, LocalDateTime.now(), AppointmentStatus.COMPLETED);

    Optional<Appointment> found = appointmentRepository.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals(AppointmentStatus.COMPLETED, found.get().getStatus());
  }

  @Test
  @DisplayName("Should delete appointment successfully")
  void delete_shouldRemoveAppointment() {

    Appointment saved =
        createAppointment(patient1, doctor1, LocalDateTime.now(), AppointmentStatus.CANCELLED);

    appointmentRepository.delete(saved);

    Optional<Appointment> deleted = appointmentRepository.findById(saved.getId());

    assertFalse(deleted.isPresent());
  }

  @Test
  @DisplayName("Should filter appointments by patient id")
  void findAllWithFilters_shouldFilterByPatient() {

    createAppointment(patient1, doctor1, LocalDateTime.now(), AppointmentStatus.SCHEDULED);

    createAppointment(patient2, doctor2, LocalDateTime.now(), AppointmentStatus.SCHEDULED);

    AppointmentFilter filter = new AppointmentFilter();

    Page<Appointment> result = appointmentRepository.findAllWithFilters(patient1.getId(), filter);

    assertEquals(1, result.getTotalElements());
    assertEquals(patient1.getId(), result.getContent().getFirst().getPatient().getId());
  }

  @Test
  @DisplayName("Should filter appointments by status and date range")
  void findAllWithFilters_shouldFilterByStatusAndDateRange() {

    LocalDateTime now = LocalDateTime.now();

    createAppointment(patient1, doctor1, now.minusDays(5), AppointmentStatus.SCHEDULED);

    createAppointment(patient1, doctor2, now.minusDays(1), AppointmentStatus.COMPLETED);

    AppointmentFilter filter = new AppointmentFilter();
    filter.setStatus(AppointmentStatus.COMPLETED);
    filter.setStartDate(LocalDate.now().minusDays(2));
    filter.setEndDate(LocalDate.now());

    Page<Appointment> result = appointmentRepository.findAllWithFilters(patient1.getId(), filter);

    assertEquals(1, result.getTotalElements());

    Appointment filtered = result.getContent().getFirst();
    assertEquals(AppointmentStatus.COMPLETED, filtered.getStatus());
    assertTrue(filtered.getStartTime().isAfter(now.minusDays(2)));
  }

  @Test
  @DisplayName("Should filter appointments by doctor id")
  void findAllWithFilters_shouldFilterByDoctor() {

    createAppointment(patient1, doctor1, LocalDateTime.now(), AppointmentStatus.SCHEDULED);

    createAppointment(patient1, doctor2, LocalDateTime.now(), AppointmentStatus.SCHEDULED);

    AppointmentFilter filter = new AppointmentFilter();
    filter.setDoctorId(doctor1.getId());

    Page<Appointment> result = appointmentRepository.findAllWithFilters(patient1.getId(), filter);

    assertEquals(1, result.getTotalElements());
    assertEquals(doctor1.getId(), result.getContent().getFirst().getDoctor().getId());
  }
}
