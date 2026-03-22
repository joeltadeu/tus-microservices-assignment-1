package com.jts.pmanagement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationTest {

  @Test
  @DisplayName("Should load Spring application context successfully")
  void contextLoads() {
    // If the context fails to start, this test will fail automatically
  }

  @Test
  @DisplayName("Main method should run without throwing exceptions")
  void main_shouldRunWithoutExceptions() {
    assertDoesNotThrow(() -> Application.main(new String[] {}));
  }
}
