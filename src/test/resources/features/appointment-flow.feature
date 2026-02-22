Feature: Appointment Business Flow

  Background:
    * url baseUrl
    * def UUID = Java.type('java.util.UUID')
    * def randomEmail =
      """
      function() {
        return 'flow_doc_' + UUID.randomUUID() + '@mail.com';
      }
      """

  Scenario: Complete appointment lifecycle

    # Create Doctor
    Given path '/v1/doctors'
    And request
    """
    {
      "firstName": "Flow",
      "lastName": "Doctor",
      "title": "Dr.",
      "specialityId": 1,
      "email": "#(randomEmail())",
      "phone": "+1-555-3333",
      "department": "General"
    }
    """
    When method POST
    Then status 201
    * def doctorId = response.id

    # Create Patient
    Given path '/v1/patients'
    And request
    """
    {
      "firstName": "Flow",
      "lastName": "Patient",
      "email": "#(randomEmail())",
      "address": "Street",
      "dateOfBirth": "1995-05-05"
    }
    """
    When method POST
    Then status 201
    * def patientId = response.id

    # Create Appointment
    Given path '/v1/patients', patientId, 'appointments'
    And request
    """
    {
      "doctorId": #(doctorId),
      "startTime": "2026-01-10T10:00:00",
      "type": "CONSULTATION",
      "title": "Consultation",
      "description": "Initial consult"
    }
    """
    When method POST
    Then status 201
    * def appointmentId = response.id
    And match response.status == "SCHEDULED"

    # Retrieve
    Given path '/v1/patients', patientId, 'appointments', appointmentId
    When method GET
    Then status 200

    # List
    Given path '/v1/patients', patientId, 'appointments'
    When method GET
    Then status 200
    And match response.content[*].id contains appointmentId

    # Cancel
    Given path '/v1/patients', patientId, 'appointments', appointmentId, 'cancel'
    And request { reason: "Patient emergency" }
    When method POST
    Then status 200
    And match response.status == "CANCELLED"

    # Create another appointment
    Given path '/v1/patients', patientId, 'appointments'
    And request
    """
    {
      "doctorId": #(doctorId),
      "startTime": "2026-02-10T10:00:00",
      "type": "CONSULTATION",
      "title": "Second",
      "description": "Second consult"
    }
    """
    When method POST
    Then status 201
    * def appointment2 = response.id

    # Delete only SCHEDULED
    Given path '/v1/patients', patientId, 'appointments', appointment2
    When method DELETE
    Then status 204