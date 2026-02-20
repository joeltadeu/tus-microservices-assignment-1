package com.jts.pmanagement.domains.patient.repository;

import com.jts.pmanagement.domains.patient.dto.PatientFilter;
import com.jts.pmanagement.domains.patient.model.Patient;
import org.springframework.data.domain.Page;

public interface PatientRepositoryCustom {
  Page<Patient> findAllWithFilters(PatientFilter filter);
}
