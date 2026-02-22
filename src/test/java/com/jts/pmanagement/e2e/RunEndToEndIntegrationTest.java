package com.jts.pmanagement.e2e;

import com.intuit.karate.junit5.Karate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RunEndToEndIntegrationTest {

  @LocalServerPort int port;

  @Karate.Test
  Karate runAll() {
    System.setProperty("karate.port", String.valueOf(port));
    return Karate.run("classpath:features").outputCucumberJson(true).relativeTo(getClass());
  }
}
