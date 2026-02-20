package com.jts.pmanagement.domains.doctor.repository;

import com.jts.pmanagement.domains.doctor.model.Speciality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialityRepository extends JpaRepository<Speciality, Long> {}
