package com.jts.pmanagement.domains.appointment.repository;

import com.jts.pmanagement.domains.appointment.dto.AppointmentFilter;
import com.jts.pmanagement.domains.appointment.model.Appointment;
import org.springframework.data.domain.Page;

public interface AppointmentRepositoryCustom {
  Page<Appointment> findAllWithFilters(Long appointmentId, AppointmentFilter filter);
}
