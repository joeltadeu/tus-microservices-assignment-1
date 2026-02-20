package com.jts.pmanagement.common.exception.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jts.pmanagement.common.exception.BadRequestException;
import com.jts.pmanagement.common.exception.ConflictException;
import com.jts.pmanagement.common.exception.NotFoundException;
import com.jts.pmanagement.common.exception.model.AttributeMessage;
import com.jts.pmanagement.common.exception.model.ExceptionResponse;
import java.nio.file.AccessDeniedException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@DisplayName("Global Exception Handler Unit Test")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when AccessDeniedException is thrown")
  void shouldHandleAccessDeniedException() throws Exception {
    AccessDeniedException ex = new AccessDeniedException("Access denied");

    ResponseEntity<ExceptionResponse> response = handler.accessDeniedException(ex);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getBody().getCode());
    assertEquals("Access denied", response.getBody().getDescription());
    assertNotNull(response.getBody().getDate());
  }

  @Test
  @DisplayName("Should return 404 Not Found when NotFoundException is thrown")
  void shouldHandleNotFoundException() {
    NotFoundException ex = new NotFoundException("Not found");

    ResponseEntity<ExceptionResponse> response = handler.notFoundException(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getCode());
    assertEquals("Not found", response.getBody().getDescription());
  }

  @Test
  @DisplayName("Should return 409 Conflict when ConflictException is thrown")
  void shouldHandleConflictException() {
    ConflictException ex = new ConflictException("Conflict");

    ResponseEntity<ExceptionResponse> response = handler.conflictException(ex);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getCode());
    assertEquals("Conflict", response.getBody().getDescription());
  }

  @Test
  @DisplayName("Should return 500 Internal Server Error for generic Exception")
  void shouldHandleGenericException() {
    Exception ex = new Exception("Internal error");

    ResponseEntity<ExceptionResponse> response = handler.exception(ex);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getCode());
    assertEquals("Internal error", response.getBody().getDescription());
  }

  @Test
  @DisplayName("Should return 400 Bad Request for IllegalArgumentException")
  void shouldHandleIllegalArgumentException() {
    IllegalArgumentException ex = new IllegalArgumentException("Illegal arg");

    ResponseEntity<ExceptionResponse> response = handler.illegalArgumentException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getCode());
    assertEquals("Illegal arg", response.getBody().getDescription());
  }

  @Test
  @DisplayName("Should return 400 Bad Request for MissingServletRequestParameterException")
  void shouldHandleMissingServletRequestParameterException() throws Exception {
    MissingServletRequestParameterException ex =
        new MissingServletRequestParameterException("id", "String");

    ResponseEntity<ExceptionResponse> response = handler.missingServletRequestParameter(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getCode());
    assertNotNull(response.getBody().getDescription());
  }

  @Test
  @DisplayName("Should return 400 Bad Request for HttpMessageNotReadableException")
  void shouldHandleHttpMessageNotReadableException() {

    HttpMessageNotReadableException ex =
        new HttpMessageNotReadableException(
            "Malformed JSON", mock(org.springframework.http.HttpInputMessage.class));

    ResponseEntity<ExceptionResponse> response = handler.httpMessageNotReadableException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Malformed JSON", response.getBody().getDescription());
  }

  @Test
  @DisplayName("Should return 400 Bad Request when BadRequestException contains message only")
  void shouldHandleBadRequestException_withMessageOnly() {
    BadRequestException ex = new BadRequestException("Bad request");

    ResponseEntity<ExceptionResponse> response = handler.handlingBadRequestException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Bad request", response.getBody().getDescription());
    assertNull(response.getBody().getAttributes());
  }

  @Test
  @DisplayName(
      "Should return 400 Bad Request when BadRequestException contains attribute errors only")
  void shouldHandleBadRequestException_withAttributesOnly() {
    List<AttributeMessage> attributes = List.of(new AttributeMessage("field", "error"));

    BadRequestException ex = new BadRequestException(attributes);

    ResponseEntity<ExceptionResponse> response = handler.handlingBadRequestException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNull(response.getBody().getDescription());
    assertEquals(1, response.getBody().getAttributes().size());
  }

  @Test
  @DisplayName("Should aggregate field errors for MethodArgumentNotValidException")
  void shouldHandleMethodArgumentNotValidException() {
    Object target = new Object();
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "object");

    bindingResult.addError(new FieldError("object", "name", "must not be null"));
    bindingResult.addError(new FieldError("object", "name", "size must be > 3"));

    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

    ResponseEntity<ExceptionResponse> response = handler.methodArgumentNotValidException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Validation Exception", response.getBody().getDescription());
    assertEquals(1, response.getBody().getAttributes().size());
  }

  @Test
  @DisplayName("Should aggregate parameter validation errors for HandlerMethodValidationException")
  void shouldHandleHandlerMethodValidationException() {
    HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);

    ParameterErrors parameterErrors = mock(ParameterErrors.class);
    MethodParameter methodParameter = mock(MethodParameter.class);

    when(methodParameter.getParameterName()).thenReturn("param");

    FieldError fieldError = new FieldError("object", "field", "must not be null");

    when(parameterErrors.getFieldErrors()).thenReturn(List.of(fieldError));
    when(parameterErrors.getMethodParameter()).thenReturn(methodParameter);
    when(ex.getParameterValidationResults())
        .thenReturn(List.of((ParameterValidationResult) parameterErrors));

    ResponseEntity<ExceptionResponse> response = handler.handlerMethodValidationException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Validation Exception", response.getBody().getDescription());
    assertFalse(response.getBody().getAttributes().isEmpty());
  }
}
