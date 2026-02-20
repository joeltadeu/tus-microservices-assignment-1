package com.jts.pmanagement.domains.doctor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jts.pmanagement.common.exception.BadRequestException;
import com.jts.pmanagement.common.exception.NotFoundException;
import com.jts.pmanagement.domains.doctor.dto.DoctorFilter;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.doctor.model.Speciality;
import com.jts.pmanagement.domains.doctor.repository.DoctorRepository;
import com.jts.pmanagement.domains.doctor.repository.SpecialityRepository;
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
@DisplayName("Doctor Service Unit Tests")
class DoctorServiceTest {

  @Mock private DoctorRepository doctorRepository;

  @Mock private SpecialityRepository specialityRepository;

  @InjectMocks private DoctorService doctorService;

  @Test
  @DisplayName("findById returns doctor when found")
  void findById_success() {
    Doctor doctor = new Doctor();
    doctor.setId(1L);

    when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

    Doctor result = doctorService.findById(1L);
    assertEquals(1L, result.getId());
    verify(doctorRepository).findById(1L);
  }

  @Test
  @DisplayName("findById throws NotFoundException when not found")
  void findById_notFound() {
    when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> doctorService.findById(1L));
  }

  @Test
  @DisplayName("findAll returns paginated doctors")
  void findAll_success() {
    DoctorFilter filter = new DoctorFilter();
    Page<Doctor> page = new PageImpl<>(Collections.singletonList(new Doctor()));
    when(doctorRepository.findAllWithFilters(filter)).thenReturn(page);

    Page<Doctor> result = doctorService.findAll(filter);
    assertEquals(1, result.getTotalElements());
    verify(doctorRepository).findAllWithFilters(filter);
  }

  @Test
  @DisplayName("insert saves doctor when email unique and speciality exists")
  void insert_success() {
    Doctor doctor = new Doctor();
    doctor.setEmail("doc@example.com");
    Speciality speciality = new Speciality();
    speciality.setId(1L);
    doctor.setSpeciality(speciality);

    when(doctorRepository.existsByEmail("doc@example.com")).thenReturn(false);
    when(specialityRepository.findById(1L)).thenReturn(Optional.of(speciality));

    doctorService.insert(doctor);

    assertNotNull(doctor.getCreatedAt());
    assertEquals(speciality, doctor.getSpeciality());
    verify(doctorRepository).save(doctor);
  }

  @Test
  @DisplayName("insert throws BadRequestException if email exists")
  void insert_duplicateEmail() {
    Doctor doctor = new Doctor();
    doctor.setEmail("doc@example.com");
    doctor.setSpeciality(new Speciality());
    when(doctorRepository.existsByEmail("doc@example.com")).thenReturn(true);

    assertThrows(BadRequestException.class, () -> doctorService.insert(doctor));
    verify(doctorRepository, never()).save(any());
  }

  @Test
  @DisplayName("insert throws BadRequestException if speciality not found")
  void insert_specialityNotFound() {
    Doctor doctor = new Doctor();
    doctor.setEmail("doc@example.com");
    Speciality speciality = new Speciality();
    speciality.setId(1L);
    doctor.setSpeciality(speciality);

    when(doctorRepository.existsByEmail("doc@example.com")).thenReturn(false);
    when(specialityRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(BadRequestException.class, () -> doctorService.insert(doctor));
    verify(doctorRepository, never()).save(any());
  }

  @Test
  @DisplayName("update modifies doctor correctly")
  void update_success() {
    Long id = 1L;
    Doctor savedDoctor = new Doctor();
    savedDoctor.setId(id);

    Doctor updatedDoctor = new Doctor();
    updatedDoctor.setEmail("new@example.com");
    updatedDoctor.setFirstName("John");
    updatedDoctor.setLastName("Doe");
    updatedDoctor.setPhone("123");
    updatedDoctor.setDepartment("Dept");
    updatedDoctor.setSpeciality(new Speciality());
    updatedDoctor.setTitle("Dr.");

    when(doctorRepository.findById(id)).thenReturn(Optional.of(savedDoctor));

    doctorService.update(id, updatedDoctor);

    assertEquals("new@example.com", savedDoctor.getEmail());
    assertEquals("John", savedDoctor.getFirstName());
    verify(doctorRepository).save(savedDoctor);
  }

  @Test
  @DisplayName("update throws NotFoundException if doctor not found")
  void update_notFound() {
    when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> doctorService.update(1L, new Doctor()));
  }

  @Test
  @DisplayName("delete removes doctor")
  void delete_success() {
    Doctor doctor = new Doctor();
    doctor.setId(1L);

    when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

    doctorService.delete(1L);

    verify(doctorRepository).delete(doctor);
  }

  @Test
  @DisplayName("delete throws NotFoundException if doctor not found")
  void delete_notFound() {
    when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> doctorService.delete(1L));
  }
}
