package com.jts.pmanagement.domains.doctor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jts.pmanagement.domains.doctor.model.Speciality;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
@DisplayName("Speciality Repository Unit Tests")
class SpecialityRepositoryImplTest {

  @Autowired private SpecialityRepository repository;

  @BeforeEach
  void setUp() {
    repository.deleteAll();

    Speciality speciality1 = Speciality.builder().description("speciality1").build();

    Speciality speciality2 = Speciality.builder().description("speciality2").build();

    Speciality speciality3 = Speciality.builder().description("speciality3").build();

    repository.save(speciality1);
    repository.save(speciality2);
    repository.save(speciality3);
  }

  @Test
  @DisplayName("save - should persist speciality and generate ID")
  void shouldSaveSpeciality() {
    Speciality speciality = Speciality.builder().description("speciality_new").build();

    Speciality saved = repository.save(speciality);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getDescription()).isEqualTo("speciality_new");
  }

  @Test
  @DisplayName("findById - should return speciality when exists")
  void shouldFindById() {
    Speciality saved =
        repository.save(Speciality.builder().description("speciality_find_by_id").build());

    Optional<Speciality> found = repository.findById(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getDescription()).isEqualTo("speciality_find_by_id");
  }

  @Test
  @DisplayName("delete - should remove speciality from database")
  void shouldDeleteSpeciality() {
    Speciality saved =
        repository.save(Speciality.builder().description("speciality_delete").build());

    repository.delete(saved);

    Optional<Speciality> deleted = repository.findById(saved.getId());

    assertThat(deleted).isEmpty();
  }
}
