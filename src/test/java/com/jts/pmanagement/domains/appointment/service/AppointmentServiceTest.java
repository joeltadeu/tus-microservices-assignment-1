package com.jts.pmanagement.domains.appointment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jts.pmanagement.common.exception.ConflictException;
import com.jts.pmanagement.common.exception.NotFoundException;
import com.jts.pmanagement.domains.appointment.dto.AppointmentFilter;
import com.jts.pmanagement.domains.appointment.dto.AppointmentRequest;
import com.jts.pmanagement.domains.appointment.dto.AppointmentStatus;
import com.jts.pmanagement.domains.appointment.dto.AppointmentType;
import com.jts.pmanagement.domains.appointment.dto.CancelAppointmentRequest;
import com.jts.pmanagement.domains.appointment.model.Appointment;
import com.jts.pmanagement.domains.appointment.repository.AppointmentRepository;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import com.jts.pmanagement.domains.doctor.service.DoctorService;
import com.jts.pmanagement.domains.patient.model.Patient;
import com.jts.pmanagement.domains.patient.service.PatientService;
import java.time.LocalDateTime;
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
@DisplayName("Appointment Service Unit Tests")
class AppointmentServiceTest {

  @Mock private DoctorService doctorService;
  @Mock private PatientService patientService;
  @Mock private AppointmentRepository appointmentRepository;

  @InjectMocks private AppointmentService appointmentService;

  @Test
  @DisplayName("findById returns appointment when found")
  void findById_success() {
    Long patientId = 1L;
    Long appointmentId = 2L;
    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setPatient(new Patient());
    appointment.getPatient().setId(patientId);

    when(appointmentRepository.findByIdAndPatientId(appointmentId, patientId))
        .thenReturn(Optional.of(appointment));

    Appointment result = appointmentService.findById(appointmentId, patientId);
    assertEquals(appointmentId, result.getId());
    verify(appointmentRepository).findByIdAndPatientId(appointmentId, patientId);
  }

  @Test
  @DisplayName("findById throws NotFoundException when not found")
  void findById_notFound() {
    Long patientId = 1L;
    Long appointmentId = 2L;

    when(appointmentRepository.findByIdAndPatientId(appointmentId, patientId))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class, () -> appointmentService.findById(appointmentId, patientId));
  }

  @Test
  @DisplayName("findAllByPatientId returns paginated results")
  void findAllByPatientId_success() {
    AppointmentFilter filter = new AppointmentFilter();
    Long patientId = 1L;

    Appointment appointment = new Appointment();
    Page<Appointment> page = new PageImpl<>(Collections.singletonList(appointment));

    when(appointmentRepository.findAllWithFilters(patientId, filter)).thenReturn(page);

    Page<Appointment> result = appointmentService.findAllByPatientId(patientId, filter);
    assertEquals(1, result.getTotalElements());
    verify(appointmentRepository).findAllWithFilters(patientId, filter);
  }

  @Test
  @DisplayName("insert sets correct fields and saves appointment")
  void insert_success() {
    Doctor doctor = new Doctor();
    doctor.setId(2L);
    Patient patient = new Patient();
    patient.setId(1L);

    Appointment appointment = new Appointment();
    appointment.setDoctor(new Doctor());
    appointment.getDoctor().setId(2L);
    appointment.setPatient(new Patient());
    appointment.getPatient().setId(1L);
    appointment.setStartTime(LocalDateTime.now());

    when(doctorService.findById(2L)).thenReturn(doctor);
    when(patientService.findById(1L)).thenReturn(patient);

    Appointment result = appointmentService.insert(appointment);

    assertAll(
        "Appointment fields",
        () -> assertEquals(doctor, result.getDoctor()),
        () -> assertEquals(patient, result.getPatient()),
        () -> assertEquals(60, result.getDuration()),
        () -> assertEquals(AppointmentStatus.SCHEDULED, result.getStatus()),
        () -> assertNotNull(result.getCreatedAt()),
        () -> assertNotNull(result.getEndTime()));

    verify(appointmentRepository).save(result);
  }

  @Test
  @DisplayName("update modifies appointment correctly")
  void update_success() {
    Long id = 1L;
    Long patientId = 2L;

    Doctor doctor = new Doctor();
    doctor.setId(5L);

    AppointmentRequest request =
        new AppointmentRequest(
            5L, LocalDateTime.now(), AppointmentType.CONSULTATION, "Checkup", "Desc");

    Appointment appointment = new Appointment();
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointment.setPatient(new Patient());
    appointment.getPatient().setId(patientId);

    when(doctorService.findById(5L)).thenReturn(doctor);
    when(patientService.findById(patientId)).thenReturn(new Patient());
    when(appointmentRepository.findByIdAndPatientId(id, patientId))
        .thenReturn(Optional.of(appointment));

    Appointment result = appointmentService.update(id, patientId, request);

    assertAll(
        "Updated fields",
        () -> assertEquals("Checkup", result.getTitle()),
        () -> assertEquals("Desc", result.getDescription()),
        () -> assertEquals(doctor, result.getDoctor()),
        () -> assertEquals(60, result.getDuration()),
        () -> assertNotNull(result.getEndTime()));
  }

  @Test
  @DisplayName("update throws ConflictException if status is not SCHEDULED")
  void update_statusConflict() {
    Long id = 1L;
    Long patientId = 2L;

    AppointmentRequest request = new AppointmentRequest(1L, LocalDateTime.now(), null, null, null);

    Appointment appointment = new Appointment();
    appointment.setStatus(AppointmentStatus.CONFIRMED);
    appointment.setPatient(new Patient());
    appointment.getPatient().setId(patientId);

    when(doctorService.findById(1L)).thenReturn(new Doctor());
    when(patientService.findById(patientId)).thenReturn(new Patient());
    when(appointmentRepository.findByIdAndPatientId(id, patientId))
        .thenReturn(Optional.of(appointment));

    assertThrows(ConflictException.class, () -> appointmentService.update(id, patientId, request));
  }

  @Test
  @DisplayName("cancel sets status, reason, and timestamps")
  void cancel_success() {
    Long id = 1L;
    Long patientId = 2L;
    CancelAppointmentRequest request = new CancelAppointmentRequest();
    request.setReason("Emergency");

    Appointment appointment = new Appointment();
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointment.setPatient(new Patient());
    appointment.getPatient().setId(patientId);

    when(appointmentRepository.findByIdAndPatientId(id, patientId))
        .thenReturn(Optional.of(appointment));

    Appointment result = appointmentService.cancel(id, patientId, request);

    assertAll(
        "Cancellation",
        () -> assertEquals(AppointmentStatus.CANCELLED, result.getStatus()),
        () -> assertEquals("Emergency", result.getCancellationReason()),
        () -> assertNotNull(result.getCancellationTime()),
        () -> assertNotNull(result.getLastUpdated()));

    verify(appointmentRepository).save(appointment);
  }

  @Test
  @DisplayName("delete removes appointment if SCHEDULED")
  void delete_success() {
    Long id = 1L;
    Long patientId = 2L;

    Appointment appointment = new Appointment();
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointment.setPatient(new Patient());
    appointment.getPatient().setId(patientId);

    when(appointmentRepository.findByIdAndPatientId(id, patientId))
        .thenReturn(Optional.of(appointment));

    appointmentService.delete(patientId, id);

    verify(appointmentRepository).delete(appointment);
  }

  @Test
  @DisplayName("delete throws ConflictException if status is not SCHEDULED")
  void delete_statusConflict() {
    Long id = 1L;
    Long patientId = 2L;

    Appointment appointment = new Appointment();
    appointment.setStatus(AppointmentStatus.CONFIRMED);
    appointment.setPatient(new Patient());
    appointment.getPatient().setId(patientId);

    when(appointmentRepository.findByIdAndPatientId(id, patientId))
        .thenReturn(Optional.of(appointment));

    assertThrows(ConflictException.class, () -> appointmentService.delete(patientId, id));
  }
}
