package com.jts.pmanagement.common.exception;

import java.io.Serial;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public class ConflictException extends RuntimeException {
  @Serial private static final long serialVersionUID = 136827568356021732L;

  private final HttpStatus status;
  private final String message;

  public ConflictException(String message) {
    this.status = HttpStatus.CONFLICT;
    this.message = message;
  }
}
