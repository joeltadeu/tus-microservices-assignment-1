package com.jts.pmanagement.domains.patient.service;

import com.jts.pmanagement.domains.patient.dto.PatientFilter;
import com.jts.pmanagement.domains.patient.model.Patient;
import com.jts.pmanagement.domains.patient.repository.PatientRepository;
import com.jts.pmanagement.common.exception.BadRequestException;
import com.jts.pmanagement.common.exception.NotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PatientService {

  private final PatientRepository repository;

  public Patient findById(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Patient with Id %s was not found".formatted(id)));
  }

  public Page<Patient> findAll(PatientFilter filter) {
    return repository.findAllWithFilters(filter);
  }

  public void insert(Patient patient) {
    log.info(
        "Before save, checking if there is another patient saved in the database with the same email [{}]",
        patient.getEmail());
    if (repository.existsByEmail(patient.getEmail())) {
      throw new BadRequestException(
          "There is another patient using the same email '%s' informed"
              .formatted(patient.getEmail()));
    }

    patient.setCreatedAt(LocalDateTime.now());
    repository.save(patient);
  }

  public void update(Long id, Patient patient) {
    log.info("Before update, checking if the patient exists...");
    var savedPatient = findById(id);
    savedPatient.setEmail(patient.getEmail());
    savedPatient.setFirstName(patient.getFirstName());
    savedPatient.setLastName(patient.getLastName());
    savedPatient.setAddress(patient.getAddress());
    savedPatient.setDateOfBirth(patient.getDateOfBirth());
    repository.save(savedPatient);
  }

  public void delete(Long id) {
    log.info("Before delete, checking if the patient exists...");
    final var patient = findById(id);

    log.info("Patient found, deleting...");
    repository.delete(patient);
  }
}
