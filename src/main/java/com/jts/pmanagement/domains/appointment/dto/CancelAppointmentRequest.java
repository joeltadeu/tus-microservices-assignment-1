package com.jts.pmanagement.domains.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CancelAppointmentRequest {
  @Schema(description = "Cancellation reason", name = "reason", example = "Patient emergency")
  private String reason;
}
