package com.jts.pmanagement.domains.doctor.repository;

import com.jts.pmanagement.domains.doctor.dto.DoctorFilter;
import com.jts.pmanagement.domains.doctor.model.Doctor;
import org.springframework.data.domain.Page;

public interface DoctorRepositoryCustom {
  Page<Doctor> findAllWithFilters(DoctorFilter filter);
}
