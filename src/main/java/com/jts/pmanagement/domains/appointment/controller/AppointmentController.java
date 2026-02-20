package com.jts.pmanagement.domains.appointment.controller;

import static com.jts.pmanagement.domains.appointment.controller.constants.AppointmentConstants.APPOINTMENT_EXAMPLE_ERROR_400_BAD_REQUEST;
import static com.jts.pmanagement.domains.appointment.controller.constants.AppointmentConstants.APPOINTMENT_EXAMPLE_ERROR_404_NOT_FOUND;
import static com.jts.pmanagement.domains.appointment.controller.constants.AppointmentConstants.EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR;

import com.jts.pmanagement.common.controller.PmsController;
import com.jts.pmanagement.domains.appointment.controller.mapper.AppointmentMapper;
import com.jts.pmanagement.domains.appointment.dto.AppointmentFilter;
import com.jts.pmanagement.domains.appointment.dto.AppointmentRequest;
import com.jts.pmanagement.domains.appointment.dto.AppointmentResponse;
import com.jts.pmanagement.domains.appointment.dto.CancelAppointmentRequest;
import com.jts.pmanagement.domains.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/patients/{patientId}/appointments")
@AllArgsConstructor
@Slf4j
public class AppointmentController implements PmsController {
  private final AppointmentService service;
  private final AppointmentMapper mapper;

  @Operation(
      summary = "Register an Appointment",
      description = "This endpoint is responsible to register a new appointment",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_CREATED,
            description = "Appointment created",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppointmentResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_BAD_REQUEST,
            description = "Appointment request is invalid",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_BAD_REQUEST_NAME,
                        description =
                            "A bad request response example when trying to register an appointment",
                        value = APPOINTMENT_EXAMPLE_ERROR_400_BAD_REQUEST)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred during register the appointment",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        description =
                            "A internal server error response example when trying to register an appointment",
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @PostMapping
  public ResponseEntity<AppointmentResponse> create(
      @PathVariable Long patientId, @RequestBody @Valid @NotNull AppointmentRequest request) {

    log.info("Creating appointment for patientId={}, payload={}", patientId, request);

    var appointment = mapper.toAppointment(patientId, request);
    var savedAppointment = service.insert(appointment);
    var response = mapper.toAppointmentResponse(savedAppointment);
    return ResponseEntity.created(getURI(savedAppointment.getId())).body(response);
  }

  @Operation(
      summary = "Update the appointment by id",
      description = "This endpoint is responsible to update the appointment by id",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH),
        @Parameter(
            name = "id",
            description = "Id of the appointment to be updated",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Appointment updated",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppointmentResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Appointment not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        description =
                            "A not found response example when trying to update an appointment does not exist",
                        value = APPOINTMENT_EXAMPLE_ERROR_404_NOT_FOUND)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred during update the appointment",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        description =
                            "A internal server error response example when trying to update an appointment",
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @PutMapping("/{id}")
  public ResponseEntity<AppointmentResponse> update(
      @PathVariable Long patientId,
      @PathVariable Long id,
      @RequestBody @Valid @NotNull AppointmentRequest request) {

    log.info("Updating appointment id={} for patientId={}, payload={}", id, patientId, request);

    var appointment = service.update(id, patientId, request);
    var response = mapper.toAppointmentResponse(appointment);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Cancel the appointment by id",
      description = "This endpoint is responsible to cancel the appointment by id",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH),
        @Parameter(
            name = "id",
            description = "Id of the appointment to be cancelled",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Appointment cancelled",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppointmentResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Appointment not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        description =
                            "A not found response example when trying to cancel an appointment does not exist",
                        value = APPOINTMENT_EXAMPLE_ERROR_404_NOT_FOUND)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred during cancel the appointment",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        description =
                            "A internal server error response example when trying to cancel an appointment",
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @PostMapping("/{id}/cancel")
  public ResponseEntity<AppointmentResponse> cancel(
      @PathVariable Long patientId, @PathVariable Long id, CancelAppointmentRequest request) {
    log.info(
        "Cancelling appointment id={} for patientId={}, reason={}",
        id,
        patientId,
        request.getReason());

    var cancelledAppointment = service.cancel(id, patientId, request);
    return ResponseEntity.ok(mapper.toAppointmentResponse(cancelledAppointment));
  }

  @Operation(
      summary = "Retrieve an appointment by id",
      description = "This endpoint is responsible to retrieve the appointment data by id",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH),
        @Parameter(
            name = "id",
            description = "Id of the appointment to be searched",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Return appointment",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppointmentResponse.class))
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Appointment not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        description =
                            "A not found response example when trying to retrieve an appointment does not exist",
                        value = APPOINTMENT_EXAMPLE_ERROR_404_NOT_FOUND)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred during retrieve the doctor",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        description =
                            "A internal server error response example when trying to retrieve a doctor",
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @GetMapping("/{id}")
  public ResponseEntity<AppointmentResponse> findById(
      @PathVariable Long patientId, @PathVariable Long id) {

    log.info("Fetching appointment id={} for patientId={}", id, patientId);

    var appointment = service.findByIdEnriched(patientId, id);
    var response = mapper.toAppointmentResponse(appointment);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Delete the appointment by id",
      description = "This endpoint is responsible to delete the appointment by id",
      security = @SecurityRequirement(name = AUTHORIZATION),
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH),
        @Parameter(
            name = "id",
            description = "Id of the appointment to be deleted",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_OK,
            description = "Appointment deleted",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_NOT_FOUND,
            description = "Appointment not found",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_NOT_FOUND_NAME,
                        description =
                            "A not found response example when trying to delete an appointment does not exist",
                        value = APPOINTMENT_EXAMPLE_ERROR_404_NOT_FOUND)
                  })
            }),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred during delete the appointment",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        description =
                            "A internal server error response example when trying to delete an appointment",
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long patientId, @PathVariable Long id) {

    log.info("Deleting appointment id={} for patientId={}", id, patientId);

    service.delete(patientId, id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Retrieve a list of appointments by patient",
      security = @SecurityRequirement(name = AUTHORIZATION),
      description =
          "This endpoint is responsible to retrieve a list of appointments by patient based on the applied filters",
      parameters = {
        @Parameter(
            name = "patientId",
            description = "Id of the patient",
            example = "1",
            in = ParameterIn.PATH)
      })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = HTTP_STATUS_CODE_OK, description = "Return appointments list"),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_UNAUTHORIZED,
            description = "Unauthorized",
            content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())}),
        @ApiResponse(
            responseCode = HTTP_STATUS_CODE_INTERNAL_SERVER_ERROR,
            description = "An unexpected error occurred during retrieve the appointments list",
            content = {
              @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  examples = {
                    @ExampleObject(
                        name = EXAMPLE_INTERNAL_SERVER_ERROR_NAME,
                        description =
                            "A internal server error response example when trying to retrieve a appointments list",
                        value = EXAMPLE_ERROR_500_INTERNAL_SERVER_ERROR)
                  })
            })
      })
  @GetMapping
  public Page<AppointmentResponse> list(@PathVariable Long patientId, AppointmentFilter filter) {

    log.info("Listing appointments for patientId={}, filter={}", patientId, filter);

    var page = service.findAllByPatientId(patientId, filter);

    log.info("Found {} appointments for patientId={}", page.getTotalElements(), patientId);

    var responseList =
        page.stream().map(mapper::toAppointmentResponse).collect(Collectors.toList());

    return new PageImpl<>(responseList, page.getPageable(), page.getTotalElements());
  }
}
