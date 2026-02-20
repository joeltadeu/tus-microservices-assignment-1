package com.jts.pmanagement.domains.patient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jts.pmanagement.common.exception.BadRequestException;
import com.jts.pmanagement.common.exception.NotFoundException;
import com.jts.pmanagement.domains.patient.dto.PatientFilter;
import com.jts.pmanagement.domains.patient.model.Patient;
import com.jts.pmanagement.domains.patient.repository.PatientRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Service Unit Tests")
class PatientServiceTest {

  @Mock private PatientRepository repository;

  @InjectMocks private PatientService patientService;

  @Test
  @DisplayName("findById returns patient when found")
  void findById_success() {
    Patient patient = new Patient();
    patient.setId(1L);

    when(repository.findById(1L)).thenReturn(Optional.of(patient));

    Patient result = patientService.findById(1L);
    assertEquals(1L, result.getId());
    verify(repository).findById(1L);
  }

  @Test
  @DisplayName("findById throws NotFoundException when not found")
  void findById_notFound() {
    when(repository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> patientService.findById(1L));
  }

  @Test
  @DisplayName("findAll returns paginated results")
  void findAll_success() {
    PatientFilter filter = new PatientFilter();
    Page<Patient> page = new PageImpl<>(Collections.singletonList(new Patient()));
    when(repository.findAllWithFilters(filter)).thenReturn(page);

    Page<Patient> result = patientService.findAll(filter);
    assertEquals(1, result.getTotalElements());
    verify(repository).findAllWithFilters(filter);
  }

  @Test
  @DisplayName("insert saves patient when email is unique")
  void insert_success() {
    Patient patient = new Patient();
    patient.setEmail("test@example.com");

    when(repository.existsByEmail("test@example.com")).thenReturn(false);

    patientService.insert(patient);

    assertNotNull(patient.getCreatedAt());
    verify(repository).save(patient);
  }

  @Test
  @DisplayName("insert throws BadRequestException if email exists")
  void insert_duplicateEmail() {
    Patient patient = new Patient();
    patient.setEmail("test@example.com");

    when(repository.existsByEmail("test@example.com")).thenReturn(true);

    assertThrows(BadRequestException.class, () -> patientService.insert(patient));
    verify(repository, never()).save(patient);
  }

  @Test
  @DisplayName("update modifies patient correctly")
  void update_success() {
    Patient savedPatient = new Patient();
    savedPatient.setId(1L);

    Patient updatedPatient = new Patient();
    updatedPatient.setEmail("new@example.com");
    updatedPatient.setFirstName("John");
    updatedPatient.setLastName("Doe");
    updatedPatient.setAddress("Address");
    updatedPatient.setDateOfBirth(LocalDate.of(1990, 1, 1));

    when(repository.findById(1L)).thenReturn(Optional.of(savedPatient));

    patientService.update(1L, updatedPatient);

    assertEquals("new@example.com", savedPatient.getEmail());
    assertEquals("John", savedPatient.getFirstName());
    verify(repository).save(savedPatient);
  }

  @Test
  @DisplayName("update throws NotFoundException if patient does not exist")
  void update_notFound() {
    Patient updatedPatient = new Patient();
    when(repository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> patientService.update(1L, updatedPatient));
  }

  @Test
  @DisplayName("delete removes patient")
  void delete_success() {
    Patient patient = new Patient();
    patient.setId(1L);

    when(repository.findById(1L)).thenReturn(Optional.of(patient));

    patientService.delete(1L);

    verify(repository).delete(patient);
  }

  @Test
  @DisplayName("delete throws NotFoundException if patient does not exist")
  void delete_notFound() {
    when(repository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> patientService.delete(1L));
  }
}
