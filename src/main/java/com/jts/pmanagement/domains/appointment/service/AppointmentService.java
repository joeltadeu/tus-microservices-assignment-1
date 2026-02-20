package com.jts.pmanagement.domains.appointment.service;

import com.jts.pmanagement.domains.appointment.dto.AppointmentFilter;
import com.jts.pmanagement.domains.appointment.dto.AppointmentRequest;
import com.jts.pmanagement.domains.appointment.dto.AppointmentStatus;
import com.jts.pmanagement.domains.appointment.dto.CancelAppointmentRequest;
import com.jts.pmanagement.domains.appointment.model.Appointment;
import com.jts.pmanagement.domains.appointment.repository.AppointmentRepository;
import com.jts.pmanagement.domains.doctor.service.DoctorService;
import com.jts.pmanagement.domains.patient.service.PatientService;
import com.jts.pmanagement.common.exception.ConflictException;
import com.jts.pmanagement.common.exception.NotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {

  private final DoctorService doctorService;
  private final PatientService patientService;
  private final AppointmentRepository appointmentRepository;

  public Appointment findByIdEnriched(Long patientId, Long id) {
    return findById(id, patientId);
  }

  public Appointment findById(Long id, Long patientId) {
    return appointmentRepository
        .findByIdAndPatientId(id, patientId)
        .orElseThrow(
            () -> new NotFoundException("Appointment with Id %s was not found".formatted(id)));
  }

  public Page<Appointment> findAllByPatientId(Long patientId, AppointmentFilter filter) {
    return appointmentRepository.findAllWithFilters(patientId, filter);
  }

  public Appointment insert(Appointment appointment) {
    // Validate doctor and patient
    var doctor = doctorService.findById(appointment.getDoctor().getId());
    var patient = patientService.findById(appointment.getPatient().getId());

    appointment.setDoctor(doctor);
    appointment.setPatient(patient);
    appointment.setCreatedAt(LocalDateTime.now());
    appointment.setEndTime(appointment.getStartTime().plusHours(1));
    appointment.setDuration(60);
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointmentRepository.save(appointment);

    return appointment;
  }

  public Appointment update(Long id, Long patientId, AppointmentRequest request) {
    // Validate doctor and patient
    var doctor = doctorService.findById(request.getDoctorId());
    patientService.findById(patientId);

    var appointment = findById(id, patientId);

    validateScheduledStatus(appointment, "updated");

    appointment.setDoctor(doctor);
    appointment.setTitle(request.getTitle());
    appointment.setDescription(request.getDescription());
    appointment.setStartTime(request.getStartTime());
    appointment.setEndTime(appointment.getStartTime().plusHours(1));
    appointment.setDuration(60);
    appointmentRepository.save(appointment);

    return appointment;
  }

  public Appointment cancel(
      Long id, Long patientId, CancelAppointmentRequest cancelAppointmentRequest) {
    log.info("Cancelling appointment {} for patient {}...", id, patientId);

    var appointment = findById(id, patientId);

    appointment.setCancellationReason(cancelAppointmentRequest.getReason());
    appointment.setCancellationTime(LocalDateTime.now());
    appointment.setLastUpdated(LocalDateTime.now());
    appointment.setStatus(AppointmentStatus.CANCELLED);

    appointmentRepository.save(appointment);
    return appointment;
  }

  public void delete(Long patientId, Long id) {
    log.info("Deleting appointment {} for patient {}...", id, patientId);

    var appointment = findById(id, patientId);

    validateScheduledStatus(appointment, "deleted");

    log.info("Appointment {} deleted successfully.", id);
    appointmentRepository.delete(appointment);
  }

  private void validateScheduledStatus(Appointment appointment, String action) {
    if (!AppointmentStatus.SCHEDULED.equals(appointment.getStatus())) {
      throw new ConflictException(
          "Appointment cannot be %s because it is not in the SCHEDULED state. Current status: '%s'."
              .formatted(action, appointment.getStatus()));
    }
  }
}
